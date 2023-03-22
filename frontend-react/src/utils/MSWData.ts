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

import { Faker, faker as _faker } from "../utils/Faker";

export const DEFAULT_SEED = 123;
export const DEFAULT_DATE = "2023-03-16T21:04:23.800Z";

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
 * Replacement for nullable() for properties that can return undefined (ex: when
 * using mocking libraries for properties that are optional).
 */
export function undefinable<T>(value: T) {
    return nullable(() => {
        const finalValue = typeof value === "function" ? value() : value;
        return finalValue === undefined ? null : finalValue;
    });
}

export interface CreateDbMockOptions {
    faker?: Faker;
    seed?: number;
    date?: number | Date | string;
}

export interface EnhancedModelAPI<
    Dictionary extends ModelDictionary,
    ModelName extends keyof Dictionary
> extends ModelAPI<Dictionary, ModelName> {
    createMany(
        num: number,
        initialValues?: InitialValues<Dictionary, ModelName>[]
    ): Entity<Dictionary, ModelName>;
}

export type EnhancedFactoryAPI<Dictionary extends ModelDictionary> = {
    [ModelName in keyof Dictionary]: EnhancedModelAPI<Dictionary, ModelName>;
} & {
    [DATABASE_INSTANCE]: Database<Dictionary>;
};

export function enhancedFactory<const Dictionary extends ModelDictionary>(
    dictionary: Dictionary
) {
    const db = factory(dictionary); //as EnhancedFactoryAPI<Dictionary>;
    for (const key in dictionary) {
        db[key] = {
            ...db[key],
            createMany(
                num: number,
                initialValues: InitialValues<Dictionary, typeof key>[] = []
            ) {
                return Array.from({ length: num }, (_, k) =>
                    this.create(initialValues[k])
                );
            },
        };
    }
    return db as EnhancedFactoryAPI<Dictionary>;
}

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
