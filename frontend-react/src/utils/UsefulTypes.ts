declare global {
    /* Makes everything string-accessible: params["id"]
     * Use generics to limit the value types of this object.
     *
     * example: StringIndexed<string | number> */
    interface StringIndexed<T = any> {
        [key: string]: T;
    }
    /* This lets us pass resource classes in. It's checking that something
     * is "newable", as in `new Object()`
     * Use generics to define what the type of `new Object()` should be.
     *
     * example: Newable<MyClass> returns an instance of MyClass */
    type Newable<T = {}> = new (...args: any[]) => T;

    /**
     * Recursively make the type readonly as regular Readonly only goes one level deep.
     */
    type RecursiveReadonly<T> = {
        readonly [P in keyof T]: T[P] extends
            | string
            | boolean
            | symbol
            | number
            | undefined
            | null
            ? T[P]
            : T[P] extends Array<infer AT>
            ? readonly RecursiveReadonly<AT>[]
            : RecursiveReadonly<T[P]>;
    };

    /**
     * Recursively make the type mutable.
     */
    type RecursiveMutable<T> = {
        [P in keyof T]: T[P] extends Readonly<
            string | boolean | symbol | number | undefined | null
        >
            ? T[P]
            : T[P] extends ReadonlyArray<infer AT>
            ? AT[]
            : RecursiveMutable<T[P]>;
    };

    type Identity<T> = T;

    /**
     * Flatten union keys into one
     */
    type Merge<T> = T extends any ? Identity<{ [k in keyof T]: T[k] }> : never;

    /**
     * Selectively require properties vs. Required
     */
    type RequiredProps<T, K extends keyof T = keyof T> = Merge<
        Omit<T, K> & {
            [P in K]-?: T[P];
        }
    >;
}

export {};
