import {
    faker as origFaker,
    Faker as OrigFaker,
    FakerOptions,
    HelpersModule as OrigHelpersModule,
} from "@faker-js/faker";

/**
 * Extended HelpersModule class that adds two useful safe methods
 * from 8.0 alpha. Faker does not export the HelpersModule class
 * from package exports so we have to cheat to get to it (
 * through an instance).
 */
class HelpersModuleBase extends Object.getPrototypeOf(origFaker.helpers)
    .constructor {
    /**
     * Helper method that converts the given number or range to a number.
     *
     * @param numberOrRange The number or range to convert.
     * @param numberOrRange.min The minimum value for the range.
     * @param numberOrRange.max The maximum value for the range.
     *
     * @example
     * faker.helpers.rangeToNumber(1) // 1
     * faker.helpers.rangeToNumber({ min: 1, max: 10 }) // 5
     *
     * @since 8.0.0
     */
    rangeToNumber(
        numberOrRange:
            | number
            | {
                  /**
                   * The minimum value for the range.
                   */
                  min: number;
                  /**
                   * The maximum value for the range.
                   */
                  max: number;
              }
    ): number {
        if (typeof numberOrRange === "number") {
            return numberOrRange;
        }

        return this.datatype.number(numberOrRange);
    }

    /**
     * Generates an array containing values returned by the given method.
     *
     * @template T The type of elements.
     * @param method The method used to generate the values.
     * @param options The optional options object.
     * @param options.count The number or range of elements to generate. Defaults to `3`.
     *
     * @example
     * faker.helpers.multiple(faker.person.firstName) // [ 'Aniya', 'Norval', 'Dallin' ]
     * faker.helpers.multiple(faker.person.firstName, { count: 3 }) // [ 'Santos', 'Lavinia', 'Lavinia' ]
     *
     * @since 8.0.0
     */
    multiple<T>(
        method: () => T,
        options: {
            /**
             * The number or range of elements to generate.
             *
             * @default 3
             */
            count?:
                | number
                | {
                      /**
                       * The minimum value for the range.
                       */
                      min: number;
                      /**
                       * The maximum value for the range.
                       */
                      max: number;
                  };
        } = {}
    ): T[] {
        const count = this.rangeToNumber(options.count ?? 3);
        if (count <= 0) {
            return [];
        }

        return Array.from({ length: count }, method);
    }

    // eslint-disable-next-line @typescript-eslint/no-useless-constructor
    constructor(faker: OrigFaker) {
        super(faker);
    }
}

/**
 * Not sure how to tell typescript that our class extends HelpersModule
 * naturally due to using prototype constructor. Remove this type and
 * use HelpersModuleBase directly when we figure out how or reach v8.0.
 */
export type HelpersModule = OrigHelpersModule & {
    rangeToNumber: HelpersModuleBase["rangeToNumber"];
    multiple: HelpersModuleBase["multiple"];
};

/**
 * Hybrid Faker with some safe features from 8.0 alpha. Allows storing
 * the defaultRefDate, but still requires passing manually to date functions
 * (8.0 will automatically use this date).
 */
export class Faker extends OrigFaker {
    readonly helpers: HelpersModule;
    private _defaultRefDate: () => Date = () => new Date();

    /**
     * Gets a new reference date used to generate relative dates.
     */
    get defaultRefDate(): () => Date {
        return this._defaultRefDate;
    }

    /**
     * Sets the `refDate` source to use if no `refDate` date is passed to the date methods.
     *
     * @param dateOrSource The function or the static value used to generate the `refDate` date instance.
     * The function must return a new valid `Date` instance for every call.
     * Defaults to `() => new Date()`.
     */
    setDefaultRefDate(
        dateOrSource: string | Date | number | (() => Date) = () => new Date()
    ): void {
        if (typeof dateOrSource === "function") {
            this._defaultRefDate = dateOrSource;
        } else {
            this._defaultRefDate = () => new Date(dateOrSource);
        }
    }

    constructor(opts: FakerOptions) {
        super(opts);
        this.helpers = new HelpersModuleBase(this) as any as HelpersModule;
    }
}

export const defaultFakerOptions = {
    locale: "en",
    localeFallback: "en",
    locales: {
        en: origFaker.locales.en,
    },
};

export function createDefaultFaker() {
    return new Faker(defaultFakerOptions);
}

export const faker = createDefaultFaker();
