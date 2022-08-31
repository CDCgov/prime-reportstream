import { useParams } from "react-router-dom";
import { useEffect } from "react";

import { StringIndexed } from "../utils/UsefulTypes";

/** Validates your incoming url params with an array of keys given as params.
 *
 * @param expectedParams {string[]} A list of string keys to check incoming params
 * */
export const useValidParams = <T>(expectedParams: string[]): T => {
    // Get all params as object
    const params = useParams();
    useEffect(() => {
        // String index the object
        const stringIndexedParams = params as StringIndexed;
        expectedParams.forEach((param) => {
            // Check all params sequentially
            if (stringIndexedParams?.[param] === undefined) {
                console.error("Missing param: ", param);
                throw Error(`Expected param at key {${param}} was undefined`);
            }
        });
    }, [expectedParams, params]);
    // Return as defined shape (this transformation makes sure returns are NOT undefined!)
    return params as unknown as T;
};
