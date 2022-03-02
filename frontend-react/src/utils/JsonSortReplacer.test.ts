import { jsonSortReplacer } from "./JsonSortReplacer";

test("SubmissionDates have valid format", () => {
    const result = JSON.stringify(
        { c: 1, a: { d: 0, c: 1, e: { a: 0, 1: 4 } } },
        jsonSortReplacer
    );
    debugger;
    expect(result).toBe(`{"a":{"c":1,"d":0,"e":{"1":4,"a":0}},"c":1}`);
});
