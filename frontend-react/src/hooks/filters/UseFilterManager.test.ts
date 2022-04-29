import { renderHook } from "@testing-library/react-hooks";

import useFilterManager, { cursorOrRange } from "./UseFilterManager";
import { RangeField } from "./UseDateRange";

describe("UseFilterManager", () => {
    test("renders with default FilterState", () => {
        const { result } = renderHook(() => useFilterManager());
        expect(result.current.rangeSettings).toEqual({
            start: "3000-01-01T00:00:00.000Z",
            end: "2000-01-01T00:00:00.000Z",
        });
        expect(result.current.sortSettings).toEqual({
            column: "",
            order: "DESC",
        });
        expect(result.current.pageSettings).toEqual({
            size: 10,
            currentPage: 1,
        });
    });
});

describe("Helper functions", () => {
    test("cursorOrRange", () => {
        const rangeAsStart = cursorOrRange(
            "ASC",
            RangeField.START,
            "cursor",
            "range"
        );
        const cursorAsStart = cursorOrRange(
            "DESC",
            RangeField.START,
            "cursor",
            "range"
        );
        const rangeAsEnd = cursorOrRange(
            "DESC",
            RangeField.END,
            "cursor",
            "range"
        );
        const cursorAsEnd = cursorOrRange(
            "ASC",
            RangeField.END,
            "cursor",
            "range"
        );

        expect(rangeAsStart).toEqual("range");
        expect(rangeAsEnd).toEqual("range");
        expect(cursorAsStart).toEqual("cursor");
        expect(cursorAsEnd).toEqual("cursor");
    });
});
