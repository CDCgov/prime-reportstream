import { jsonSourceMap, SourceMapResult } from "./JsonSourceMap";
import { splitOn } from "./misc";

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
 * Technically, "level1" is different because "leaf2" buried under it changed, so the diff is
 * ["/", "/level2", "/level2/leaf2"]
 * But really we only want ["/level2/leaf2"]
 *
 * The function removes the parent path elements
 * @param pathArray
 */
const extractLeafNodes = (pathArray: string[]): string[] => {
    // we want to remove parent paths from changed elements.
    if (!pathArray.length) {
        return [];
    }

    // iterate backwards since leaf nodes will be last.
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

export const jsonDiffer = (
    leftData: SourceMapResult,
    rightData: SourceMapResult,
    leafNodesOnly: boolean = true
): JsonDiffResult => {
    // diff the keys. If the key is different, then just consider the value of that key to be different.
    const leftKeys = Object.keys(leftData.pointers);
    const rightKeys = Object.keys(rightData.pointers);

    // this is O(3n^2) because includes() rescans whole index. If we were dealing with large amounts of data
    // this can be done in O(n) using two cursors since the data is assumed to be sorted.
    const addedLeftKeys = leftKeys.filter(
        (key) => key.length && !rightKeys.includes(key)
    );
    const addedRightKeys = rightKeys.filter(
        (key) => key.length && !leftKeys.includes(key)
    );

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
    const getLeftValueFunc = (key: string): string =>
        leftData.json.slice(
            leftData.pointers[key].value.pos,
            leftData.pointers[key].valueEnd.pos
        );

    const getRightValueFunc = (key: string): string =>
        rightData.json.slice(
            rightData.pointers[key].value.pos,
            rightData.pointers[key].valueEnd.pos
        );

    let changedKeys = intersection.filter(
        (key) => getLeftValueFunc(key) !== getRightValueFunc(key)
    );

    if (leafNodesOnly) {
        return {
            addedLeftKeys,
            addedRightKeys,
            changedKeys: extractLeafNodes(changedKeys),
        };
    }

    return {
        addedLeftKeys,
        addedRightKeys,
        changedKeys,
    };
};

const insertMark = (s1: string, start: number, end: number): string => {
    if (s1 === "" || end - start <= 0 || end >= s1.length) {
        return s1;
    }
    const three_parts = splitOn(s1, start, end);
    if (three_parts.length !== 3) {
        return s1;
    }
    return `${three_parts[0]}<mark>${three_parts[1]}</mark>${three_parts[2]}`;
};

type JsonDifferMarkupResult = {
    left: { normalizedJson: string; markupText: string };
    right: { normalizedJson: string; markupText: string };
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
): JsonDifferMarkupResult => {
    const leftMap = jsonSourceMap(leftJson, 2);
    const rightMap = jsonSourceMap(rightJson, 2);
    const diffs = jsonDiffer(leftMap, rightMap);

    // The trick here is that we collect all the markup, then apply it
    // working backwards so we don't screw with the offsets.
    // e.g. "1, 2, 3, 4" vs "0, 1, 2, 5" if we highlight the  "0" on the right
    // by inserting  "<mark>0</mark>, 1, 2, 5" then the offset for "5" has changed!
    // but if we start at the end then the earlier offsets are the same.
    // e.g. "0, 1, 2, <mark>5</mark>", the offset for "0" isn't changed when we get to it.

    type Markers = {
        start: number;
        end: number;
    };

    const leftDiffs: Markers[] = diffs.addedLeftKeys.map((key) => ({
        start:
            leftMap.pointers[key].key?.pos || leftMap.pointers[key].value.pos,
        end: leftMap.pointers[key].valueEnd.pos,
    }));

    const rightDiffs: Markers[] = diffs.addedRightKeys.map((key) => ({
        start:
            rightMap.pointers[key].key?.pos || rightMap.pointers[key].value.pos,
        end: rightMap.pointers[key].valueEnd.pos,
    }));

    // for the value changes, just hightlight the value not keys. We'll push these into the existing array
    // off offsets, but then we'll need to sort so we can work backwards.
    for (const eachKey of diffs.changedKeys) {
        leftDiffs.push({
            start: leftMap.pointers[eachKey].value.pos,
            end: leftMap.pointers[eachKey].valueEnd.pos,
        });

        rightDiffs.push({
            start: rightMap.pointers[eachKey].value.pos,
            end: rightMap.pointers[eachKey].valueEnd.pos,
        });
    }

    // left and right changes markers need the same operations, this function is abstracted out
    // to reduce code.
    const processMarkersFunc = (
        markers: Markers[],
        jsonText: string
    ): string => {
        if (markers.length > 1) {
            // sort forwards for doing overlap matches
            markers.sort((a, b) => a.start - b.start);

            // find and remove overlaps.
            // There are still cases where values (vs keys) can over overlap.
            // {key: "a"} vs {key: [1,2,3]}
            // This is because the JSonSourceMap treats each array value as a separate sub-value.
            // Which is nice for spotting differences in large arrays in json.
            // Tested by "jsonDifferMarkup value type switched
            markers = markers.reduce(
                (acc: Markers[], value: Markers, index, array) => {
                    if (acc.length === 0) {
                        acc.push(value);
                    } else if (value.start >= acc[acc.length - 1].end) {
                        acc.push(value);
                    }
                    return acc;
                },
                [] as Markers[]
            );
            // sort backwards for doing mark inserts into the text
            markers.sort((a, b) => b.start - a.start);
        }

        // finally, we add the marks to the text. again, working backwards.
        let markupText = jsonText;
        for (const eachDiff of markers) {
            markupText = insertMark(markupText, eachDiff.start, eachDiff.end);
        }

        return markupText;
    };

    const leftMarkupText = processMarkersFunc(leftDiffs, leftMap.json);
    const rightMarkupText = processMarkersFunc(rightDiffs, rightMap.json);

    return {
        left: { normalizedJson: leftMap.json, markupText: leftMarkupText },
        right: {
            normalizedJson: rightMap.json,
            markupText: rightMarkupText,
        },
    };
};

export const _exportForTestingJsonDiffer = {
    extractLeafNodes,
    insertMark,
};
