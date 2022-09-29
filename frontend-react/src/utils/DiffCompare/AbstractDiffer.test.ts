import { insertMark } from "./AbstractDiffer";

describe("AbstractDiff test suite", () => {
    test("insertMark Basic test", () => {
        expect(insertMark("1234567890", 0, 3)).toBe("<mark>123</mark>4567890");
    });

    test("insertMark bounds tests", () => {
        // start and means zero length
        expect(insertMark("1234567890", 1, 1)).toBe("1234567890");
        // past end of string
        expect(insertMark("1234567890", 0, 20)).toBe("1234567890");
        // end is before start
        expect(insertMark("1234567890", 5, 1)).toBe("1234567890");
    });
});
