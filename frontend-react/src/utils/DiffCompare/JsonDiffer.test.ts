import { jsonSourceMap } from "./JsonSourceMap";
import {
    _exportForTestingJsonDiffer,
    jsonDiffer,
    jsonDifferMarkup,
} from "./JsonDiffer";

describe("JsonDiffer test suite - depends on jsonSourceMap working", () => {
    test("isInPath utility function", () => {
        expect(
            _exportForTestingJsonDiffer.isInPath("/foo/bar/1", "/foo1/bar")
        ).toBe(false);
        expect(
            _exportForTestingJsonDiffer.isInPath("/foo/bar/12", "/foo/bar/1")
        ).toBe(false);
        expect(
            _exportForTestingJsonDiffer.isInPath("/foo/bar/1/2/3", "/foo/bar/1")
        ).toBe(true);
    });

    test("isNotInAnyPath utility function", () => {
        expect(
            _exportForTestingJsonDiffer.isNotInAnyPath("/foo/bar/2", [
                "/foo",
                "/foo/bar",
            ])
        ).toBe(false);

        expect(
            _exportForTestingJsonDiffer.isNotInAnyPath("/foo/bar/2", [
                "/foo1",
                "/foo1/bar",
            ])
        ).toBe(true);

        expect(
            _exportForTestingJsonDiffer.isNotInAnyPath("/foo/bar/2", [])
        ).toBe(true);
    });

    test("extractLeafNodes() utility function", () => {
        // one node
        const results = _exportForTestingJsonDiffer.extractLeafNodes([
            "",
            "/1",
            "/1/2",
            "/1/2/3",
        ]);
        expect(results).toStrictEqual(["/1/2/3"]);

        // two nodes
        const results2 = _exportForTestingJsonDiffer.extractLeafNodes([
            "",
            "/1",
            "/1/2",
            "/1/2/3",
            "/1/2/4",
        ]);
        expect(results2).toStrictEqual(["/1/2/3", "/1/2/4"]);
    });

    test("insertMarks nested", () => {
        const result = _exportForTestingJsonDiffer.insertMarks("abcdefg", [
            { start: 0, end: 7 },
            { start: 3, end: 4 },
        ]);
        expect(result).toStrictEqual("<mark>abc<mark>d</mark>efg</mark>");
    });

    test("jsonDiffer Basic test", () => {
        const left = jsonSourceMap({ key: "value" }, 2);
        const right = jsonSourceMap({ key: "VALUE" }, 2);
        const diffs = jsonDiffer(left, right);
        expect(diffs.addedLeftKeys).toStrictEqual([]);
        expect(diffs.addedRightKeys).toStrictEqual([]);
        expect(diffs.changedKeys).toStrictEqual(["/key"]);
    });

    test("jsonDiffer keys test", () => {
        const left = jsonSourceMap({ key1: "value" }, 2);
        const right = jsonSourceMap({ key2: "value" }, 2);
        {
            const diffs = jsonDiffer(left, right);
            expect(diffs.addedLeftKeys).toStrictEqual(["/key1"]);
            expect(diffs.addedRightKeys).toStrictEqual(["/key2"]);
            // "" because if keys changed the root node's content is different!
            expect(diffs.changedKeys).toStrictEqual([]);
        }
        {
            const trimmedDiffs = jsonDiffer(left, right);
            expect(trimmedDiffs.addedLeftKeys).toStrictEqual(["/key1"]);
            expect(trimmedDiffs.addedRightKeys).toStrictEqual(["/key2"]);
            expect(trimmedDiffs.changedKeys).toStrictEqual([]);
        }
    });

    test("jsonDiffer combo test", () => {
        const left = jsonSourceMap(
            { addleft: "addedleft", same: "same", diff: "leftdiff" },
            2
        );
        const right = jsonSourceMap(
            { addright: "addedright", same: "same", diff: "rightdiff" },
            2
        );
        const diffs = jsonDiffer(left, right);
        expect(diffs.addedLeftKeys).toStrictEqual(["/addleft"]);
        expect(diffs.addedRightKeys).toStrictEqual(["/addright"]);
        expect(diffs.changedKeys).toStrictEqual(["/diff"]);
    });

    test("jsonDifferMarkup diff values only", () => {
        const valueDiffs = jsonDifferMarkup(
            { key1: "value1" },
            { key1: "value2" }
        );
        expect(valueDiffs.left.markupText).toBe(
            `{\n  "key1": <mark>"value1"</mark>\n}`
        );
        expect(valueDiffs.right.markupText).toBe(
            `{\n  "key1": <mark>"value2"</mark>\n}`
        );
        // valueDiffs.left.json and valueDiffs.right.json check in other tests
    });

    test("jsonDifferMarkup diff keys only", () => {
        // diff: key1, key3. Same: key2
        const keyDiffs = jsonDifferMarkup(
            { key1: "value1", key2: "value2" },
            { key2: "value2", key3: "value3" }
        );

        expect(keyDiffs.left.markupText).toBe(
            `{\n  <mark>"key1": "value1"</mark>,\n  "key2": "value2"\n}`
        );

        expect(keyDiffs.right.markupText).toBe(
            `{\n  "key2": "value2",\n  <mark>"key3": "value3"</mark>\n}`
        );
    });

    test("jsonDifferMarkup diff keys and values different not nested", () => {
        const comboDiffs = jsonDifferMarkup(
            {
                key1: "value1",
                key2: "value2",
                key3: "value3-left",
                key4: "value4",
            },
            {
                key0: "value0",
                key2: "value2",
                key3: "value3-right",
                key5: "value5",
            }
        );

        expect(comboDiffs.left.markupText).toBe(
            `{\n  <mark>"key1": "value1"</mark>,\n  "key2": "value2",\n  "key3": <mark>"value3-left"</mark>,\n  <mark>"key4": "value4"</mark>\n}`
        );
        expect(comboDiffs.right.markupText).toBe(
            `{\n  <mark>"key0": "value0"</mark>,\n  "key2": "value2",\n  "key3": <mark>"value3-right"</mark>,\n  <mark>"key5": "value5"</mark>\n}`
        );
    });

    test("jsonDifferMarkup value type switched", () => {
        const diff = jsonDifferMarkup({ key: [1, 2, 3] }, { key: "a" });
        expect(diff.left.markupText).toBe(
            `{\n  "key": <mark>[\n    <mark>1</mark>,\n    <mark>2</mark>,\n    <mark>3</mark>\n  ]</mark>\n}`
        );
        expect(diff.right.markupText).toBe(`{\n  "key": <mark>"a"</mark>\n}`);
    });

    test("jsonDifferMarkup values in array", () => {
        const diff = jsonDifferMarkup(
            { key: [1, 3, 4, 6] },
            { key: [2, 3, 5, 6] }
        );
        expect(diff.left.markupText).toBe(
            `{\n  "key": [\n    <mark>1</mark>,\n    3,\n    <mark>4</mark>,\n    6\n  ]\n}`
        );
        expect(diff.right.markupText).toBe(
            `{\n  "key": [\n    <mark>2</mark>,\n    3,\n    <mark>5</mark>,\n    6\n  ]\n}`
        );
    });
    test("jsonDifferMarkup diff keys and values different nested", () => {
        const nestedDiff = jsonDifferMarkup(
            {
                key1: {
                    key12: [1, 2, 3, 4],
                },
                key2: "value2",
                key3: "same",
                key4: "diff key same content",
            },
            {
                key1: {
                    key12: "completely different type",
                },
                key2: {
                    key22: "nested object NOT string",
                },
                key3: "same",
                key5: "diff key same content",
            }
        );

        expect(nestedDiff.left.markupText).toBe(
            `{\n  "key1": {\n    "key12": <mark>[\n      <mark>1</mark>,\n      <mark>2</mark>,\n      <mark>3</mark>,\n      <mark>4</mark>\n    ]</mark>\n  },\n  "key2": <mark>"value2"</mark>,\n  "key3": "same",\n  <mark>"key4": "diff key same content"</mark>\n}`
        );
        expect(nestedDiff.right.markupText).toBe(
            `{\n  "key1": {\n    "key12": <mark>"completely different type"</mark>\n  },\n  "key2": <mark>{\n    <mark>"key22": "nested object NOT string"</mark>\n  }</mark>,\n  "key3": "same",\n  <mark>"key5": "diff key same content"</mark>\n}`
        );
    });

    test("Nested key change", () => {
        const nestedDiff = jsonDifferMarkup(
            {
                createdBy: "test@example.com",
                timing: {
                    whenEmpty: {
                        onlyOncePerDay: false,
                    },
                },
            },
            {
                createdBy: "test@example.com",
                timing: {
                    whenEmpty1: {
                        onlyOncePerDay: false,
                    },
                },
            }
        );

        // expect(nestedDiff.left.markupText).toBe(
        //     `{\n  "createdBy": "test@example.com",\n  "timing": {\n    <mark>"whenEmpty": {\n      "onlyOncePerDay": false\n    }</mark>\n  }\n}`
        // );
        // expect(nestedDiff.right.markupText).toBe(
        //     `{\n  "createdBy": "test@example.com",\n  "timing": {\n    <mark>"whenEmpty1": {\n      "onlyOncePerDay": false\n    }</mark>\n  }\n}`
        // );
    });
});
