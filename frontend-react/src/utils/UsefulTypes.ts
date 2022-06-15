/* Makes everything string-accessible: params["id"]
 * Use generics to limit the value types of this object.
 *
 * example: StringIndexed<string | number> */
export interface StringIndexed<T = any> {
    [key: string]: T;
}
/* This lets us pass resource classes in. It's checking that something
 * is "newable", as in `new Object()`
 * Use generics to define what the type of `new Object()` should be.
 *
 * example: Newable<MyClass> returns an instance of MyClass */
export type Newable<T = {}> = new (...args: any[]) => T;
