import { nullable, factory, drop } from "@mswjs/data";
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
    defaultContext,
    ResponseComposition,
} from "msw";

import { Faker, faker as _faker } from "../utils/Faker";

export const DEFAULT_SEED = 123;
export const DEFAULT_DATE = "2023-03-16T21:04:23.800Z";

export type RequestParams<Key extends PrimaryKeyType> = {
    [K in Key]: string;
};

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

export type FactoryHandlerMap<T = unknown> = Record<
    "getList" | "get" | "post" | "put" | "delete",
    T
>;
export type PartialFactoryHandlerMap<T = unknown> = Partial<
    FactoryHandlerMap<T>
>;

export type RelativePathMap = PartialFactoryHandlerMap<string>;
export type ParentResolverMap = PartialFactoryHandlerMap<
    ResponseResolver<any, any>
>;

export interface EnhancedRestHandlersOptions {
    relativePaths?: FactoryHandlerMap<string>;
    parentHandlers: FactoryHandlerMap;
}

export type RestHandlerMethod = string | RegExp;

type DefaultContext = typeof defaultContext;

export interface ProxyRequestContext extends DefaultContext {
    proxy: {
        res?: any;
        error?: Error;
    };
}

export class ProxyRestHandler extends RestHandler {
    target: any;
    proxyResolver: any;

    constructor(
        method: RestHandlerMethod,
        path: Path,
        target: RestHandler,
        resolver?: ResponseResolver<any, any>
    ) {
        const callback = (
            req: MockedRequest,
            res: ResponseComposition<any>,
            ctx: DefaultContext
        ) => this.resolve(req, res, ctx);
        super(method, path, callback as ResponseResolver<any, any>);
        this.target = target;
        this.proxyResolver = resolver ?? defaultProxyResolver;
    }

    async resolve(
        req: MockedRequest,
        res: ResponseComposition<any>,
        ctx: DefaultContext
    ) {
        const resolved: ProxyRequestContext["proxy"] = {
            res: undefined,
            error: undefined,
        };
        const proxyReq = await this.createProxyRequest(req);

        try {
            resolved.res = (await this.target.run(proxyReq)).response;
        } catch (e: any) {
            resolved.error = e;
        }

        const proxyCtx = {
            ...ctx,
            proxy: resolved,
        };

        return this.proxyResolver(req, res, proxyCtx);
    }

    async createProxyRequest(req: MockedRequest) {
        return new MockedRequest(new URL(this.target.info.path.toString()), {
            ...req,
            body: await req.arrayBuffer(),
        });
    }
}

export const FactoryRestHandlerTupleMap = {
    0: "getList",
    1: "get",
    2: "post",
    3: "put",
    4: "delete",
} as const;

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

export async function defaultProxyResolver(_: any, _1: any, ctx: any) {
    if (ctx.proxy.error) {
        throw ctx.proxy.error;
    }
    return ctx.proxy.res;
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
            // Getters that return JSON serializable data
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

export interface CreateDbMockOptions {
    faker?: Faker;
    seed?: number;
    date?: number | Date | string;
}

/**
 * Takes a callback and options. Calls callback with faker instance to use
 * to receive ModelDictionary (whose type information has not been lost and
 * generified to ModelDictionary). Returns db, faker instance, and a reset
 * helper.
 */
export function createDbMock<const Dictionary extends ModelDictionary>(
    cb: (faker: Faker) => Dictionary,
    {
        date = DEFAULT_DATE,
        seed = DEFAULT_SEED,
        faker = _faker,
    }: CreateDbMockOptions = {}
) {
    const models = cb(faker);
    const db = enhancedFactory(models);

    /**
     * Empty the db and reset faker sequence.
     */
    function reset() {
        drop(db);
        faker.seed(seed);
        faker.setDefaultRefDate(date);
    }

    reset();

    return {
        db,
        faker,
        reset,
    };
}
