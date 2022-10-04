import { textDifferMarkup } from "./TextDiffer";

describe("JsonDiffer test suite - depends on jsonSourceMap working", () => {
    test("Basic functionality", () => {
        const left = JSON.stringify({ key: "value" }, null, 2);
        const right = JSON.stringify({ key: "VALUE" }, null, 2);
        const diffs = textDifferMarkup(left, right);
        expect(diffs.left.markupText).toStrictEqual(
            `{\n  "key": "<mark>value</mark>"\n}`
        );
        expect(diffs.right.markupText).toStrictEqual(
            `{\n  "key": "<mark>VALUE</mark>"\n}`
        );
    });
});
