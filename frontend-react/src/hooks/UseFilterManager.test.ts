import { act, renderHook } from "@testing-library/react-hooks";

import useFilterManager, { FilterState } from "./UseFilterManager";

const emptyFilters: FilterState = {
    dateRange: {
        startRange: "",
        endRange: "",
    },
    sort: { column: "", order: "DESC" },
    pageSize: 10,
};

describe("UseFilterManager", () => {
    test("Renders with default FilterState", () => {
        const { result } = renderHook(() => useFilterManager());
        expect(result.current.filters.dateRange.endRange).toEqual(
            "2020-01-01T00:00:00.000Z"
        );
        expect(result.current.filters.sort.column).toEqual("");
        expect(result.current.filters.sort.order).toEqual("DESC");
        expect(result.current.filters.pageSize).toEqual(10);
    });

    test("FilterController functions update the state", () => {
        const { result } = renderHook(() => useFilterManager());
        act(() =>
            result.current.update.setRange(
                new Date("2999-01-01T00:00:00.000Z"),
                new Date("2020-01-01T00:00:00.000Z")
            )
        );
        expect(result.current.filters.dateRange.endRange).toEqual(
            "2020-01-01T00:00:00.000Z"
        );
        expect(result.current.filters.dateRange.startRange).toEqual(
            "2999-01-01T00:00:00.000Z"
        );

        act(() => result.current.update.setSortSettings("test"));
        expect(result.current.filters.sort.column).toEqual("test");

        // Mimic's calling setSortSettings for same column, swaps sort.order
        act(() => result.current.update.setSortSettings("test"));
        expect(result.current.filters.sort.order).toEqual("ASC");

        act(() => result.current.update.setPageSize(50));
        expect(result.current.filters.pageSize).toEqual(50);

        act(() => result.current.update.clearAll());
        expect(result.current.filters.dateRange.endRange).toEqual(
            "2020-01-01T00:00:00.000Z"
        );
        expect(result.current.filters.sort).toEqual(emptyFilters.sort);
        expect(result.current.filters.pageSize).toEqual(emptyFilters.pageSize);
    });
});
