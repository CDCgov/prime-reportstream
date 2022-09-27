import { SourceMapResult } from "./JsonSourceMap";

/**
 * Leverages jsonSourceMap to diff two jsons.
 * Key concepts:
 * 1. Keys are not "changed". They are added or removed from left/right sides.
 *    e.g. { key1: "test" } --> { key2: "test } is really adding "key1" to left and adding "key2" to right.
 *    To simplify we say keys are only added to a side (versus removed from the other side)
 * 2. Values can be changed if they keys are the same.
 * 3. Code depends on JsonSourceMap to convert the json into an easily parsable structure.
 * 4. The calling code can then combine the results of the jsonDiffer to hightlight differences.
 * 5. hierarchical json will appear as multiple changes.
 *    e.g. ANY change in any content will cause the root node "/" to show up.
 *         Or, if there's a path `/array/1` then `/array` will also appear.
 *    In theory, the caller could change highlight colors as it goes down deeper.
 *    There is a utility class to coalesce these differnces
 *
 * @param rightData SourceMapResult
 * @param leftData SourceMapResult
 * @return JsonDiffResult addedRightKeys
 */

export type JsonDiffResult = {
    addedLeftKeys: string[];
    addedRightKeys: string[];
    changedKeys: string[];
};

export const jsonDiffer = (
    leftData: SourceMapResult,
    rightData: SourceMapResult,
    leafNodesOnly: boolean = false
): JsonDiffResult => {
    // diff the keys. If the key is different, then just consider the value of that key to be different.
    const leftKeys = Object.keys(leftData.pointers);
    const rightKeys = Object.keys(rightData.pointers);

    // this is O(3n^2) because includes() rescans whole index. If we were dealing with large amounts of data
    // this can be done in O(n) using two cursors since the data is assumed to be sorted.
    const addedLeftKeys = leftKeys.filter((key) => !rightKeys.includes(key));
    const addedRightKeys = rightKeys.filter((key) => !leftKeys.includes(key));

    // now we want intersection (aka NOT changed and see if the values have changed).
    let intersection = leftKeys.filter((key) => rightKeys.includes(key));

    // now things get more complex, we pull out the value of unchanged keys and see if that's different.
    // we use the `pointers` structure to pull out the value from the `json`
    // pointers.value && pointers.valueEnd
    // export interface SourceMapResult {
    //     json: string;
    //     pointers: Pointers;
    // }
    // inline func() improves readability only. slices out the string of a given value
    const getLeftValue = (key: string) =>
        leftData.json.slice(
            leftData.pointers[key].value.pos,
            leftData.pointers[key].valueEnd.pos
        );

    const getRightValue = (key: string) =>
        rightData.json.slice(
            rightData.pointers[key].value.pos,
            rightData.pointers[key].valueEnd.pos
        );

    let changedKeys = intersection.filter(
        (key) => !getLeftValue(key).includes(getRightValue(key))
    );

    if (leafNodesOnly) {
        // we want to remove parent paths from changed elements.

        // iterate backwards since leaf node will be last.
        // But, we can remove elements as we go, so hav
        for (const ii = changedKeys.length - 1; ii >= 0; ii--) {
            changedKeys = changedKeys.filter((key) => {

            };
        }
    }

    return {
        addedLeftKeys,
        addedRightKeys,
        changedKeys,
    };
};
