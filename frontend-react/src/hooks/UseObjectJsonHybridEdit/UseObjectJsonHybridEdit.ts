import { CheckJsonError, checkJson } from "../../utils/misc";

export type ObjectJsonHybrid<
    T extends Record<string, any>,
    K extends keyof T | undefined = undefined,
> = {
    [k in keyof T]: k extends K
        ? string
        : K extends undefined
        ? T[k] | string
        : T[k];
};

export type ObjectJsonHybridParse<
    T extends Record<string, any>,
    K extends keyof T | undefined = undefined,
> = {
    [k in keyof T]: k extends K ? any : T[k];
};

export function isObjStringEmpty(str: string) {
    return str === "null" || str === "[]" || str === "{}";
}

export function isObjEmpty(obj: object | unknown[] | undefined | null) {
    if (obj == null) return true;
    if (Array.isArray(obj)) {
        if (obj.length === 0) return true;
        return false;
    }
    if (Object.keys(obj).length === 0) return true;
    return false;
}

function stringify(v: any) {
    const str = JSON.stringify(v, undefined, 2);
    return str;
}

export function createJsonHybrid<
    const T extends Record<string, any>,
    const K extends (keyof T)[] | undefined,
>(
    obj: T,
    jsonKeys?: K,
): ObjectJsonHybrid<T, K extends (keyof T)[] ? K[number] : undefined> {
    return {
        ...Object.fromEntries(
            Object.entries(obj).map(([k, v]) => {
                if (jsonKeys?.includes(k as any)) {
                    const s = stringify(v);
                    return [k, s];
                }
                return [k, v];
            }),
        ),
    } as any;
}

export interface CheckJsonHybridError {
    name: string;
    error: string | CheckJsonError;
}

export interface CheckJsonHybridResult<
    T extends Record<string, any> = any,
    K extends keyof T | undefined = undefined,
> {
    obj: ObjectJsonHybridParse<T, K>;
    errors: CheckJsonHybridError[];
}

export function checkJsonHybrid<
    T extends Record<string, any>,
    K extends keyof T | undefined,
>(
    hybrid: ObjectJsonHybrid<T, K>,
    jsonKeys: (keyof T)[],
): CheckJsonHybridResult<T, K> {
    const errors: CheckJsonHybridError[] = [];
    const obj = Object.fromEntries(
        Object.entries(hybrid)
            .map(([k, v]) => {
                if (jsonKeys.includes(k as Exclude<typeof k, string>)) {
                    if (v) {
                        if (typeof v !== "string") {
                            // likely temporary during field registration/hybrid object processing
                            errors.push({ name: k, error: "Expected string" });
                            return [k, v];
                        }
                        const { obj, error } = checkJson(v);
                        if (error) {
                            errors.push({ name: k, error: error.msg });
                            return [k, undefined];
                        }
                        return [k, obj];
                    }
                    return [k, undefined];
                }
                return [k, v];
            })
            .filter(Boolean),
    ) as T;
    return { obj, errors };
}

export function cleanupJsonObject<T extends Record<string, any>>(obj: T): T {
    return Object.fromEntries(
        Object.entries(obj).filter(([, v]) => !isObjEmpty(v)),
    ) as T;
}
