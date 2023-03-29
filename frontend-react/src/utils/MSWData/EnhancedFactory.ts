import { nullable, factory } from "@mswjs/data";
import { Database } from "@mswjs/data/lib/db/Database";
import {
    ModelValueTypeGetter,
    NestedModelDefinition,
    PrimaryKeyType,
    ModelValueType,
    ModelAPI,
    ModelDictionary,
    DATABASE_INSTANCE,
    InitialValues,
    Entity,
} from "@mswjs/data/lib/glossary";
import { NullableProperty } from "@mswjs/data/lib/nullable";
import { PrimaryKey } from "@mswjs/data/lib/primaryKey";
import { OneOf, ManyOf } from "@mswjs/data/lib/relations/Relation";
import {
    ResponseResolver,
    RestHandler,
    Path,
    DefaultBodyType,
    MockedRequest,
} from "msw";

import { ProxyRestHandler } from "./ProxyRestHandler";

export type TypedModelDefinitionValue<T> =
    | (T extends PrimaryKeyType ? PrimaryKey<T> : never)
    | ModelValueTypeGetter
    | (T extends ModelValueType ? NullableProperty<T> : never)
    | (T extends KeyType ? OneOf<T, boolean> | ManyOf<T, boolean> : never)
    | NestedModelDefinition;

/**
 * Javascript type -> DB Model type
 * Property types updated to support callables. Optional properties
 * converted to required properties that can return null.
 */
export type DBFactoryModel<T> = {
    [key in keyof T]-?: Extract<T[key], undefined> extends undefined
        ? TypedModelDefinitionValue<
              NonNullable<T[key]> | null | (() => NonNullable<T[key]> | null)
          >
        : TypedModelDefinitionValue<T[key]>;
};

/**
 * DB Entity type -> original javascript type
 * Returns the original type provided to DBFactoryModel to represent
 * the entity as it would be when json serialized (this assumes the
 * the type provided was already json-safe!).
 */
export type JSONEntity<T extends Entity<any, any> | null> = T extends Entity<
    infer Dictionary,
    infer ModelName
>
    ? Dictionary[ModelName] extends DBFactoryModel<infer M>
        ? M
        : never
    : never;

/**
 * Replacement for nullable() for properties that can return undefined (ex: when
 * using mocking libraries for properties that are optional).
 */
export function undefinable<T>(value: T) {
    return nullable(() => {
        const finalValue = typeof value === "function" ? value() : value;
        return finalValue === undefined ? null : finalValue;
    });
}

export type RestHandlerMethods = "getList" | "get" | "post" | "put" | "delete";

export type FactoryHandlerMap<T = unknown> = Record<
    RestHandlerMethods,
    T
>;

export type PartialFactoryHandlerMap<T = unknown> = Partial<
    FactoryHandlerMap<T>
>;

export interface EnhancedModelAPI<
    Dictionary extends ModelDictionary,
    ModelName extends keyof Dictionary
> extends ModelAPI<Dictionary, ModelName> {
    createMany(
        num: number,
        initialValues?: InitialValues<Dictionary, ModelName>[]
    ): Entity<Dictionary, ModelName>;
    toEnhancedRestHandlers<
        const P extends PartialFactoryHandlerMap,
        const R extends PartialFactoryHandlerMap
    >(
        baseUrl: string,
        relativePathMap?: P,
        parentResolversMap?: R
    ): RestHandler<MockedRequest<DefaultBodyType>>[];
    getAllJson: () => JSONEntity<ReturnType<this["getAll"]>[0]>[];
    findFirstJson: (
        ...args: Parameters<this["findFirst"]>
    ) => JSONEntity<ReturnType<this["findFirst"]>> | null;
    findManyJson: (
        ...args: any[]
    ) => JSONEntity<ReturnType<this["findMany"]>[0]>;
}

export type EnhancedFactoryAPI<Dictionary extends ModelDictionary> = {
    [ModelName in keyof Dictionary]: EnhancedModelAPI<Dictionary, ModelName>;
} & {
    [DATABASE_INSTANCE]: Database<Dictionary>;
};

export type RelativePathMap = PartialFactoryHandlerMap<string>;
export type ParentResolverMap = PartialFactoryHandlerMap<
    ResponseResolver<any, any>
>;

export interface EnhancedRestHandlersOptions {
    relativePaths?: PartialFactoryHandlerMap<string>;
    parentHandlers: PartialFactoryHandlerMap;
}

export const FactoryRestHandlerTupleMap = {
    0: "getList",
    1: "get",
    2: "post",
    3: "put",
    4: "delete",
} as const satisfies {[k: number]: RestHandlerMethods};

export function factoryRestHandlerMapToTuple<const T>(
    map: PartialFactoryHandlerMap<T>
) {
    return [
        map[FactoryRestHandlerTupleMap[0]],
        map[FactoryRestHandlerTupleMap[1]],
        map[FactoryRestHandlerTupleMap[2]],
        map[FactoryRestHandlerTupleMap[3]],
        map[FactoryRestHandlerTupleMap[4]],
    ];
}

export function factoryRestHandlerTupleToMap(tuple: RestHandler[]): FactoryHandlerMap<RestHandler> {
    const [getListHandler, getHandler, postHandler, putHandler, deleteHandler] = tuple;
    return {
        getList: getListHandler,
        get: getHandler,
        post: postHandler,
        put: putHandler,
        delete: deleteHandler
    };
}

/**
 * Factory with our custom additions (ex: createMany).
 */
export function enhancedFactory<const Dictionary extends ModelDictionary>(
    dictionary: Dictionary
) {
    const db = factory(dictionary) as EnhancedFactoryAPI<Dictionary>;
    for (const key in dictionary) {
        db[key] = {
            ...db[key],
            /**
             * Create many helper function. Creates {num} entities with first
             * {initialValues}.length initialized with provided values and the
             * rest fully mocked.
             */
            createMany(
                num: number,
                initialValues: InitialValues<Dictionary, typeof key>[] = []
            ) {
                return Array.from({ length: num }, (_, k) =>
                    this.create(initialValues[k])
                );
            },
            /**
             * Create enhanced rest handlers.
             */
            toEnhancedRestHandlers<
                const P extends RelativePathMap,
                const R extends ParentResolverMap
            >(baseUrl: string, relativePathMap?: P, parentResolversMap?: R) {
                const parentResolvers = factoryRestHandlerMapToTuple(
                    parentResolversMap ?? {}
                );
                const relativePaths = factoryRestHandlerMapToTuple(
                    relativePathMap ?? {}
                );
                const handlers = this.toHandlers("rest", baseUrl).map(
                    (h, i) => {
                        const parentResolver = parentResolvers[i];
                        const method = h.info.method;
                        const relativePath = relativePaths[i];

                        if (parentResolver || relativePath) {
                            const fullUrl = relativePath
                                ? `${baseUrl}${relativePath}`
                                : h.info.path.toString();
                            return new ProxyRestHandler(
                                method,
                                fullUrl as Path,
                                h,
                                parentResolver
                            );
                        }

                        return h;
                    }
                );

                return handlers;
            },
            // Getters that return JSON serializable data. Entities from the original
            // functions could have metadata (symbol properties) that will fail an
            // expect equal comparison with json data from its rest handler.
            getAllJson() {
                return JSON.parse(JSON.stringify(this.getAll()));
            },
            findFirstJson(
                ...args: Parameters<
                    (typeof db)[keyof typeof db & string]["findFirst"]
                >
            ) {
                const record = this.findFirst(...(args as [any]));
                if (record === null) {
                    return null;
                }

                return JSON.parse(JSON.stringify(record));
            },
            findManyJson(
                ...args: Parameters<
                    (typeof db)[keyof typeof db & string]["findMany"]
                >
            ) {
                return JSON.parse(
                    JSON.stringify(this.findMany(...(args as [any])))
                );
            },
        };
    }
    return db;
}
