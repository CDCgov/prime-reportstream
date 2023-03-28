import { drop } from "@mswjs/data";
import { PrimaryKeyType, ModelDictionary } from "@mswjs/data/lib/glossary";

import { Faker, faker as _faker } from "../Faker";

import { enhancedFactory } from "./EnhancedFactory";

export const DEFAULT_SEED = 123;
export const DEFAULT_DATE = "2023-03-16T21:04:23.800Z";

export type RequestParams<Key extends PrimaryKeyType> = {
    [K in Key]: string;
};

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
