import { jsonSortReplacer } from "./JsonSortReplacer";

test("SubmissionDates have valid format", () => {
    const result = JSON.stringify(
        { c: 1, a: { d: 0, c: 1, e: { a: 0, 1: 4 } } },
        jsonSortReplacer
    );
    expect(result).toBe(`{"a":{"c":1,"d":0,"e":{"1":4,"a":0}},"c":1}`);

    // now try arrays with nested arrays
    const result2 = JSON.stringify(
        { c: 1, a: [["c", "b", "a"], 1, 3, ["cc", "bb", "aa"]] },
        jsonSortReplacer
    );
    debugger;
    expect(result2).toBe(`{"a":[1,3,["a","b","c"],["aa","bb","cc"]],"c":1}`);
});
