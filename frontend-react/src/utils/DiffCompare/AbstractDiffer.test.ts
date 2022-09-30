import { insertMark } from "./AbstractDiffer";
import { _exportForTestingJsonDiffer } from "./JsonDiffer";

describe("AbstractDiff test suite", () => {
    test("insertMark Basic test", () => {
        expect(insertMark("1234567890", 0, 3)).toBe("<mark>123</mark>4567890");
    });

    const str = "Optimize for results not optics";
    test("insertMark() function normal", () => {
        expect(
            _exportForTestingJsonDiffer.insertMark(str, 13, 7)
        ).toStrictEqual("Optimize for <mark>results</mark> not optics");
    });

    test("insertMark() expected failures", () => {
        // starts past end of string
        expect(
            _exportForTestingJsonDiffer.insertMark(str, str.length + 1, 1)
        ).toStrictEqual(str);

        // no range (zero length selection)
        expect(_exportForTestingJsonDiffer.insertMark(str, 0, 0)).toStrictEqual(
            str
        );

        // empty string
        expect(_exportForTestingJsonDiffer.insertMark("", 0, 0)).toStrictEqual(
            ""
        );
    });
});
