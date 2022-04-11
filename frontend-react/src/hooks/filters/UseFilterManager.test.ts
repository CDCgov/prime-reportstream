import { act, renderHook } from "@testing-library/react-hooks";

import useFilterManager from "./UseFilterManager";

describe("UseFilterManager", () => {
    test("Renders with default FilterState", () => {
        const { result } = renderHook(() => useFilterManager());
        expect(result.current.range.endRange.toISOString()).toEqual(
            "2000-01-01T00:00:00.000Z"
        );
        expect(result.current.sort.column).toEqual("");
        expect(result.current.sort.order).toEqual("DESC");
        expect(result.current.pageSize.count).toEqual(10);
    });

    test("FilterController functions update the state", () => {
        const { result } = renderHook(() => useFilterManager());
        act(() =>
            result.current.range.controller.set({
                date1: "2999-01-01",
                date2: "2020-01-01",
            })
        );
        expect(result.current.range.endRange.toISOString()).toEqual(
            "2020-01-01T00:00:00.000Z"
        );
        expect(result.current.range.startRange.toISOString()).toEqual(
            "2999-01-01T00:00:00.000Z"
        );

        act(() => result.current.sort.set("test"));
        expect(result.current.sort.column).toEqual("test");
        expect(result.current.sort.order).toEqual("ASC");

        act(() => result.current.pageSize.set(50));
        expect(result.current.pageSize.count).toEqual(50);

        act(() => result.current.clearAll());

        expect(result.current.range.endRange.toISOString()).toEqual(
            "2000-01-01T00:00:00.000Z"
        );
        expect(result.current.sort.column).toEqual("");
        expect(result.current.sort.order).toEqual("DESC");
        expect(result.current.pageSize.count).toEqual(10);
    });
});
