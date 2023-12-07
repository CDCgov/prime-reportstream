import { useCallback, useEffect, useMemo, useState } from "react";

import { CheckJsonError, checkJson } from "../../utils/misc";
import { Simplify } from "type-fest";

export type ObjectJsonHybrid<
    T,
    K extends (string & keyof T) | undefined = undefined,
> = {
    [k in keyof T]: k extends K ? string : T[k];
};

function stringify(v: any) {
    return JSON.stringify(v, undefined, 2);
}

export function createJsonHybrid<T extends {} = any>(
    obj: T,
    jsonKeys?: (string & keyof T)[],
): Simplify<
    ObjectJsonHybrid<
        T,
        typeof jsonKeys extends (string & keyof T)[]
            ? (typeof jsonKeys)[number]
            : undefined
    >
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
                return [k, v];
            }),
        ),
    } as any;
}

export interface CheckJsonHybridError {
    name: string;
    error: string | CheckJsonError;
}

export interface CheckJsonHybridResult<T = any> {
    obj: T;
    errors: CheckJsonHybridError[];
}

export function checkJsonHybrid<T extends object = any>(
    hybrid: T,
    jsonKeys: string[],
): CheckJsonHybridResult<T> {
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
                        errors.push({ name: k, error });
                        return [k, undefined];
                    }
                    return [k, obj];
                }
                return [k, undefined];
            }
            return [k, v];
        }),
    ) as typeof hybrid;
    return { obj, errors };
}

/**
 * Hook that handles converting an initial value into an editable hybrid json object where
 * all matching properties from `jsonKeys` are json stringified. This hybrid is simultaneously
 * checked per property and (if possible) converted back into the original shape. The original
 * shape of the edited object is stringified in `json` for editing itself. If changes to `json` are
 * valid, it is converted and set as the hybrid object.
 */
export default function useObjectJsonHybridEdit<T extends object = any>(
    initialValue: T,
    jsonKeys: (string & keyof T)[],
) {
    const _createJsonHybrid = useCallback(
        (v: T) => createJsonHybrid(v, jsonKeys),
        [jsonKeys],
    );
    const [hybrid, setHybrid] = useState(() => _createJsonHybrid(initialValue));
    const [json, setJson] = useState(() => stringify(initialValue));
    const hybridResult = useMemo(
        () => checkJsonHybrid(hybrid, jsonKeys),
        [hybrid, jsonKeys],
    );
    const jsonResult = useMemo(() => checkJson<T>(json), [json]);

    useEffect(() => {
        setHybrid((h) => createJsonHybrid(h as any, jsonKeys));
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
