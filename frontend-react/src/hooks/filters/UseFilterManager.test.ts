import { renderHook } from "@testing-library/react-hooks";

import useFilterManager, { cursorOrRange } from "./UseFilterManager";
import { RangeField } from "./UseDateRange";

describe("UseFilterManager", () => {
    test("renders with default FilterState", () => {
        const { result } = renderHook(() => useFilterManager({}));
        expect(result.current.rangeSettings).toEqual({
            to: "3000-01-01T00:00:00.000Z",
            from: "2000-01-01T00:00:00.000Z",
        });
        expect(result.current.sortSettings).toEqual({
            column: "",
            order: "DESC",
            locally: false,
            localOrder: "DESC",
        });
        expect(result.current.pageSettings).toEqual({
            size: 10,
            currentPage: 1,
        });
    });

    test("renders with given defaults", () => {
        const { result } = renderHook(() =>
            useFilterManager({
                sortDefaults: {
                    column: "testCol",
                    order: "ASC",
                    locally: true,
                    localOrder: "ASC",
                },
            })
        );
        expect(result.current.rangeSettings).toEqual({
            to: "3000-01-01T00:00:00.000Z",
            from: "2000-01-01T00:00:00.000Z",
        });
        expect(result.current.sortSettings).toEqual({
            column: "testCol",
            order: "ASC",
            locally: true,
            localOrder: "ASC",
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
            RangeField.TO,
            "cursor",
            "range"
        );
        const cursorAsStart = cursorOrRange(
            "DESC",
            RangeField.TO,
            "cursor",
            "range"
        );
        const rangeAsEnd = cursorOrRange(
            "DESC",
            RangeField.FROM,
            "cursor",
            "range"
        );
        const cursorAsEnd = cursorOrRange(
            "ASC",
            RangeField.FROM,
            "cursor",
            "range"
        );

        expect(rangeAsStart).toEqual("range");
        expect(rangeAsEnd).toEqual("range");
        expect(cursorAsStart).toEqual("cursor");
        expect(cursorAsEnd).toEqual("cursor");
    });
});
