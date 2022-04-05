import { act, renderHook } from "@testing-library/react-hooks";

import useFilterManager from "./UseFilterManager";

describe("UseFilterManager", () => {
    test("Renders with default FilterState", () => {
        const { result } = renderHook(() => useFilterManager());
        expect(result.current.filters.startRange).toEqual("");
        expect(result.current.filters.endRange).toEqual("");
        expect(result.current.filters.sortOrder).toEqual("DESC");
        expect(result.current.filters.pageSize).toEqual(10);
    });

    test("FilterController functions update the state", () => {
        const { result } = renderHook(() => useFilterManager());

        act(() => result.current.update.setStartRange("04/07/1970"));
        expect(result.current.filters.startRange).toEqual("04/07/1970");

        act(() => result.current.update.setEndRange("12/31/9998"));
        expect(result.current.filters.endRange).toEqual("12/31/9998");

        act(() => result.current.update.swapSortOrder());
        expect(result.current.filters.sortOrder).toEqual("ASC");

        act(() => result.current.update.setPageSize(50));
        expect(result.current.filters.pageSize).toEqual(50);
    });
});
