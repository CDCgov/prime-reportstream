/* Makes everything string-accessible: params["id"]
 * Use generics to limit the value types of this object.
 *
 * example: StringIndexed<string | number> */
export type StringIndexed<T = any> = Record<string, T>;
/* This lets us pass resource classes in. It's checking that something
 * is "newable", as in `new Object()`
 * Use generics to define what the type of `new Object()` should be.
 *
 * example: Newable<MyClass> returns an instance of MyClass */
export type Newable<T = object> = new (...args: any[]) => T;
/* Useful for when a function catches errors and could possibly
 * return something other than the desired object type
 *
 * example: MyObjectError extends SimpleError { ... }
 * example: a return type of MyObject | SimpleError  */
export class SimpleError {
    message: string;
    constructor(message: string) {
        this.message = message;
    }
}

export enum PERMISSIONS {
    /* Non-Okta pseudo groups */
    SENDER = "DHSender",
    RECEIVER = "DHReceiver",
    /* Okta groups */
    PRIME_ADMIN = "DHPrimeAdmins",
}
