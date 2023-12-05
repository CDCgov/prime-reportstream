import { DifferMarkupResult } from "../../utils/DiffCompare/AbstractDiffer";
import { jsonDifferMarkup } from "../../utils/DiffCompare/JsonDiffer";
import { textDifferMarkup } from "../../utils/DiffCompare/TextDiffer";

/**
 * Get diff markup for a set of two objects or strings.
 */
function useJsonDiff(a: string, b: string): DifferMarkupResult;
function useJsonDiff(a: object, b: object): DifferMarkupResult;
function useJsonDiff(a: unknown, b: unknown): DifferMarkupResult {
    if (typeof a !== typeof b)
        throw new TypeError("Both items of set must be of the same type");
    const diffMarkup =
        typeof a !== "string"
            ? jsonDifferMarkup(a, b)
            : textDifferMarkup(a, b as string);

    return diffMarkup;
}

export default useJsonDiff;
