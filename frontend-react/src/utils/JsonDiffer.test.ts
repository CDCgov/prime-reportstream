import { jsonSourceMap } from "./JsonSourceMap";
import { jsonDiffer } from "./JsonDiffer";

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
});
