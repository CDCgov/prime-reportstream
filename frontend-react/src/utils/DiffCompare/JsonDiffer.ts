import { DifferMarkupResult } from "./AbstractDiffer";
import { jsonSourceMap, SourceMapResult } from "./JsonSourceMap";

/**
 * Leverages jsonSourceMap to diff two jsons.
 * Key concepts:
 * - Simplification: keys are not "changed". They are added to the left or right sides.
 *    e.g. { key1: "test" } --> { key2: "test } is really adding "key1" to left and adding "key2" to right.
 *    To simplify we say keys are only added to a side (versus removed from the other side)
 * - Values can be changed if they keys are the same.
 * - Code depends on JsonSourceMap to convert the json into an easily parsable structure.
 * - The calling code can then combine the results of the jsonDiffer to hightlight differences.
 *   This is done below via `jsonDifferMarkup()`
 * - Hierarchical json will appear as multiple changes.
 *    e.g. ANY change in any content will cause the root node "/" to show up.
 *         Or, if there's a path `/array/1` then `/array` will also appear.
 *    In theory, the caller could change highlight colors as it goes down deeper.
 *    There is a utility class to coalesce these differences
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

/**
 * Compare to key paths and determine if the path is a child of the parent.
 * one liner, but increases readability.
 * @param childPath path to check. format ("/top/middle/bottom")
 * @param parentPath path that is considered parent ("/top/middle")
 */
const isInPath = (childPath: string, parentPath: string) =>
    `${childPath}/`.startsWith(`${parentPath}/`);

/**
 * Loop over array of parentPaths and if childPath has any parentPaths as the
 * parent return true.
 * Not a great algo because it doesn't stop as soon as there's a match, but ok for
 * small amounts of data.
 */
const isNotInAnyPath = (childPath: string, parentPaths: string[]) =>
    parentPaths.filter((p) => isInPath(childPath, p)).length === 0;

/**
 * Technically, if a nested node changes in json, then all parent nodes
 * are different as well. For a simple differ (with a single color hightlight for diffs),
 * we only care about the leaf nodes (the actual keys that changed, not the whole parent path)
 *
 * e.g. Diff these jsons:
 * {
 *   level1: {
 *     leaf1: "v1",
 *     level2: {
 *        leaf2: "v1",
 *     }
 * }
 *
 * {
 *   level1: {
 *     leaf1: "v1",
 *     level2: {
 *        leaf2: "v2",
 *     }
 * }
 *
 * Technically, "level1" is different because "leaf2" buried under it changed
 * This function does this:
 * ["/", "/level2", "/level2/leaf2"] => ["/level2/leaf2"]
 *
 * The function removes the parent path elements
 * @param pathArray
 */
const extractLeafNodes = (pathArray: string[]): string[] => {
    // we want to remove parent paths from changed elements.
    if (!pathArray.length) {
        return [];
    }

    // iterate BACKWARDS since leaf nodes will be last.
    // But, we can remove elements as we go, so have refresh the array or we'll get burned
    // by being out of bounds
    let ii = pathArray.length - 1;
    while (ii >= 0) {
        const lengthBefore = pathArray.length; // if len change, then restart from end.
        const leafPath = pathArray[ii] || "";
        // /1/2/3/4/5 => ["", "/1", "/1/2", "/1/2/3", "/1/2/3/4", "/1/2/3/4/5"];
        const parentPaths: string[] = leafPath
            .split("/")
            .map((elem, index, array) =>
                [...array.slice(0, index), elem].join("/")
            )
            .slice(0, -1); // remove the last element which is leaf node itself

        // now remove all parents from the array.
        pathArray = pathArray.filter((s) => !parentPaths.includes(s));
        // if nothing was removed, then move to back one item, otherwise, start over at end.
        ii = lengthBefore === pathArray.length ? ii - 1 : pathArray.length - 1;
    }

    // special case: OF COURSE the root node changes if ANYTHING under it has changed (keys or values)
    // But this isn't actually what we want, so just remove it.
    if (pathArray.length && pathArray[0] === "") {
        pathArray.shift();
    }

    return pathArray;
};

type Marker = {
    start: number;
    end: number;
};

/**
 * Given a list of keys into the json map, produce and array of start/end markers
 * This is just the value section
 */
const convertValuesToMarkers = (
    keys: string[],
    jsonMap: SourceMapResult
): Marker[] => {
    return keys.reduce(
        (acc: Marker[], each: string): Marker[] => [
            ...acc,
            {
                start: jsonMap.pointers[each].value.pos,
                end: jsonMap.pointers[each].valueEnd.pos,
            },
        ],
        []
    );
};

/**
 * Given a list of keys into the json map, produce and array of start/end markers.
 * This is the *start of the key* to the end of the value
 */
const convertNodesToMarkers = (
    keys: string[],
    jsonMap: SourceMapResult
): Marker[] => {
    return keys.reduce(
        (acc: Marker[], each: string): Marker[] => [
            ...acc,
            {
                start:
                    jsonMap.pointers[each].key?.pos ||
                    jsonMap.pointers[each].value.pos,
                end: jsonMap.pointers[each].valueEnd.pos,
            },
        ],
        []
    );
};

// Split out for unit testing
// The trick here is that we collect all the markup, then apply it
// working backwards so we don't screw with the offsets.
// e.g. "1, 2, 3, 4" vs "0, 1, 2, 5" if we highlight the  "0" on the right
// by inserting  "<mark>0</mark>, 1, 2, 5" then the offset for "5" has changed!
// but if we start at the end then the earlier offsets are the same.
// e.g. "0, 1, 2, <mark>5</mark>", the offset for "0" isn't changed when we get to it.
const insertMarks = (startStr: string, markers: Marker[]): string => {
    type MarkerInsert = {
        pos: number;
        mark: "<mark>" | "</mark>";
    };
    // turn into a single MarkerInsert[]. This enables easy back-to-front inserting into string
    // we insert two entries per mark into the accumlator array
    const inserts = markers.reduce(
        (acc, each: Marker): MarkerInsert[] => [
            ...acc,
            { pos: each.start, mark: "<mark>" },
            { pos: each.end, mark: "</mark>" },
        ],
        [] as MarkerInsert[]
    );

    // we reverse sort by pos MUST work from back to front
    inserts.sort((a, b) => b.pos - a.pos);

    // go through and inject <mark> or </mark> at each pos
    return inserts.reduce(
        (acc: string, each: MarkerInsert) =>
            `${acc.slice(0, each.pos)}${each.mark}${acc.slice(each.pos)}`,
        startStr
    );
};

/**
 * Diff compares two jsons and returns a map of the differences. You probably want to use
 * @param leftData
 * @param rightData
 */
export const jsonDiffer = (
    leftData: SourceMapResult,
    rightData: SourceMapResult
): JsonDiffResult => {
    // diff the keys. If the key is different, then just consider the value of that key to be different.
    const leftKeys = Object.keys(leftData.pointers);
    const rightKeys = Object.keys(rightData.pointers);

    // this is looking for diffs between the two lists.
    let addedLeftKeys = leftKeys.filter(
        (key) => key.length && !rightKeys.includes(key)
    );
    let addedRightKeys = rightKeys.filter(
        (key) => key.length && !leftKeys.includes(key)
    );

    // now we want intersection (aka NOT changed and see if the values have changed).
    let intersection = leftKeys.filter((key) => rightKeys.includes(key));

    // for readability improvements only, pull out start/end values
    const getStartEnd = (key: string, data: SourceMapResult) => {
        return [data.pointers[key].value.pos, data.pointers[key].valueEnd.pos];
    };
    // inline getValue() improves readability only. slices out the string of a given value
    const getValue = (key: string, data: SourceMapResult): string =>
        data.json.slice(...getStartEnd(key, data));

    // now things get more complex, we pull out the value of unchanged keys and see if that's different.
    // we use the `pointers` structure to pull out the value from the `json`
    // pointers.value && pointers.valueEnd
    // export interface SourceMapResult {
    //     json: string;
    //     pointers: JsonMapPointers;
    // }
    let changedKeys = intersection.filter(
        (key) =>
            key !== "" && getValue(key, leftData) !== getValue(key, rightData)
    );

    // now extract just the node leaves from the keys. This is because technically the content of each parent
    // has changed, but we really only think about the leaf nodes as being diff
    changedKeys = extractLeafNodes(changedKeys);

    // so `{level1: { level2: { level3: "value"} } }` vs `{level1: { level2: { level3MOD: "value"} } }`
    // The keys `/level/level2/level3` (left) and  `/level1/level2/level3MOD` (right) are different,
    // BUT so is the PARENT VALUE `/level1/level2`
    // so we go back and remove changedKeys from the addedKeys
    // But we needed the intersection of the two sets to find the changedKeys earlier.
    if (changedKeys.length) {
        addedLeftKeys = extractLeafNodes([
            ...addedLeftKeys,
            ...changedKeys,
        ]).filter((key) => addedLeftKeys.includes(key));

        addedRightKeys = extractLeafNodes([
            ...addedRightKeys,
            ...changedKeys,
        ]).filter((key) => addedRightKeys.includes(key));
    }

    return {
        addedLeftKeys,
        addedRightKeys,
        changedKeys,
    };
};

/**
 * Given two json find the difference, then return a normalized json string and one with <mark>s
 * added for the differences.
 *
 * @param leftJson Valid Json object
 * @param rightJson Valid Json object
 */
export const jsonDifferMarkup = (
    leftJson: any,
    rightJson: any
): DifferMarkupResult => {
    // left and right should be json objects, but there's really no way to typescript enforce it.
    if (typeof leftJson === "string" || typeof rightJson === "string") {
        console.log("mean to pass simple strings versus json objects");
    }
    const leftMap = jsonSourceMap(leftJson, 2);
    const rightMap = jsonSourceMap(rightJson, 2);
    const diffs = jsonDiffer(leftMap, rightMap);

    // collect all the markers, then insert marks
    const leftMarks = [
        ...convertNodesToMarkers(diffs.addedLeftKeys, leftMap),
        ...convertValuesToMarkers(diffs.changedKeys, leftMap),
    ];
    const leftMarkupText = insertMarks(leftMap.json, leftMarks);

    const rightMarks = [
        ...convertNodesToMarkers(diffs.addedRightKeys, rightMap),
        ...convertValuesToMarkers(diffs.changedKeys, rightMap),
    ];
    const rightMarkupText = insertMarks(rightMap.json, rightMarks);

    return {
        left: { normalized: leftMap.json, markupText: leftMarkupText },
        right: {
            normalized: rightMap.json,
            markupText: rightMarkupText,
        },
    };
};

export const _exportForTestingJsonDiffer = {
    extractLeafNodes,
    insertMarks,
    isInPath,
    isNotInAnyPath,
};
