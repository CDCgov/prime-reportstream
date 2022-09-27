import { jsonSourceMap } from "./JsonSourceMap";
import { _exportForTestingJsonDiffer, jsonDiffer } from "./JsonDiffer";

describe("JsonDiffer test suite - depends on jsonSourceMap", () => {
    test("JsonSourceMap Basic test", () => {
        const left = jsonSourceMap({ key: "value" }, 2);
        const right = jsonSourceMap({ key: "VALUE" }, 2);
        const diffs = jsonDiffer(left, right);
        expect(diffs.addedLeftKeys).toStrictEqual([]);
        expect(diffs.addedRightKeys).toStrictEqual([]);
        expect(diffs.changedKeys).toStrictEqual(["", "/key"]);
    });

    test("JsonSourceMap combo test", () => {
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
        expect(diffs.changedKeys).toStrictEqual(["", "/diff"]);
    });

    test("JsonSourceMap  ", () => {
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
});
