import { useParams } from "react-router-dom";
import { useEffect } from "react";

import { StringIndexed } from "../utils/UsefulTypes";

/** Validates your incoming url params with an array of keys given as params.
 * All params from {@link useParams} return as possibly undefined, despite defining
 * them as not undefined. This lets us define which to throw an error on if they are
 * not present in the url.
 *
 * @param requiredParams {string[]} A list of string keys to check incoming params
 * */
export const useValidParams = <T>(requiredParams: string[]): T => {
    // Get all params as object
    const params = useParams();
    useEffect(() => {
        // String index the object
        const stringIndexedParams = params as StringIndexed;
        requiredParams.forEach((param) => {
            // Check all params sequentially
            if (stringIndexedParams?.[param] === undefined) {
                console.error("Missing param: ", param);
                throw Error(`Expected param at key {${param}} was undefined`);
            }
        });
    }, [requiredParams, params]);
    // Return as defined shape (this transformation makes sure returns are NOT undefined!)
    return params as unknown as T;
};
