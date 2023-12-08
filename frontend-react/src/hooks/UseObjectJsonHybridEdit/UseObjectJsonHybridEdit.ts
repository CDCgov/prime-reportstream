import { useCallback, useEffect, useMemo, useState } from "react";
import { Simplify } from "type-fest";

import { CheckJsonError, checkJson } from "../../utils/misc";

export type ObjectJsonHybrid<
    T extends Record<string, any>,
    K extends keyof T | undefined = undefined,
> = {
    [k in keyof T]: K extends undefined
        ? T[k] | string
        : k extends K
        ? string
        : T[k];
};

export type ObjectJsonHybridParse<
    T extends Record<string, any>,
    K extends keyof T | undefined = undefined,
> = {
    [k in keyof T]: k extends K ? any : T[k];
};

function stringify(v: any) {
    return JSON.stringify(v, undefined, 2);
}

export function createJsonHybrid<T extends Record<string, any>>(
    obj: T,
    jsonKeys?: (keyof T)[],
): ObjectJsonHybrid<
    T,
    typeof jsonKeys extends (keyof T)[] ? (typeof jsonKeys)[number] : undefined
> {
    return {
        ...Object.fromEntries(
            Object.entries(obj).map(([k, v]) => {
                if (jsonKeys?.includes(k as any)) {
                    let s = stringify(v);
                    if (s === "null" || s === "[]" || s === "{}") {
                        s = "";
                    }
                    return [k, s];
                }
                return [k, v === null ? undefined : v];
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
    H extends Record<string, any>,
    T extends Record<string, any> = H extends ObjectJsonHybrid<infer t>
        ? t
        : never,
>(
    hybrid: H,
    jsonKeys: (keyof T)[],
): CheckJsonHybridResult<T, (typeof jsonKeys)[number]> {
    const errors: CheckJsonHybridError[] = [];
    const obj = Object.fromEntries(
        Object.entries(hybrid).map(([k, v]) => {
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
        }),
    ) as T;
    return { obj, errors };
}

/**
 * Hook that handles converting an initial value into an editable hybrid json object where
 * all matching properties from `jsonKeys` are json stringified. This hybrid is simultaneously
 * checked per property and (if possible) converted back into the original shape. The original
 * shape of the edited object is stringified in `json` for editing itself. If changes to `json` are
 * valid, it is converted and set as the hybrid object.
 */
export default function useObjectJsonHybridEdit<
    T extends Record<string, any> = any,
>(initialValue: T, jsonKeys: (keyof T)[]) {
    const [hybrid, setHybrid] = useState(() =>
        createJsonHybrid(initialValue, jsonKeys),
    );
    const [json, setJson] = useState(() => stringify(initialValue));
    const hybridResult = useMemo(
        () => checkJsonHybrid<T>(hybrid, jsonKeys),
        [hybrid, jsonKeys],
    );
    const jsonResult = useMemo(() => checkJson<T>(json), [json]);

    useEffect(() => {
        setHybrid((h) => createJsonHybrid(h, jsonKeys));
    }, [jsonKeys]);

    useEffect(() => {
        const { obj } = hybridResult;
        if (obj) {
            setJson(stringify(obj));
        }
    }, [hybridResult]);

    useEffect(() => {
        const { obj } = jsonResult;
        if (obj) {
            setHybrid(createJsonHybrid(obj, jsonKeys));
        }
    }, [jsonResult, jsonKeys]);

    return {
        hybrid,
        setHybrid,
        json,
        setJson,
        hybridResult,
        jsonResult,
    };
}
