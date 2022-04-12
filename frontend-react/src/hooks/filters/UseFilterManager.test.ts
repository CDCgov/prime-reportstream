import { act, renderHook } from "@testing-library/react-hooks";

import useFilterManager from "./UseFilterManager";

describe("UseFilterManager", () => {
    test("Renders with default FilterState", () => {
        const { result } = renderHook(() => useFilterManager());
        expect(result.current.endRange.toISOString()).toEqual(
            "2000-01-01T00:00:00.000Z"
        );
        expect(result.current.column).toEqual("");
        expect(result.current.order).toEqual("DESC");
        expect(result.current.count).toEqual(10);
    });

    test("FilterController functions update the state", () => {
        const { result } = renderHook(() => useFilterManager());
        act(() =>
            result.current.setRange({
                date1: "2999-01-01",
                date2: "2020-01-01",
            })
        );
        expect(result.current.endRange.toISOString()).toEqual(
            "2020-01-01T00:00:00.000Z"
        );
        expect(result.current.startRange.toISOString()).toEqual(
            "2999-01-01T00:00:00.000Z"
        );

        act(() => result.current.setSort("test", "DESC"));
        expect(result.current.column).toEqual("test");
        expect(result.current.order).toEqual("ASC");

        act(() => result.current.setCount(50));
        expect(result.current.count).toEqual(50);

        act(() => result.current.resetAll());

        expect(result.current.endRange.toISOString()).toEqual(
            "2000-01-01T00:00:00.000Z"
        );
        expect(result.current.column).toEqual("");
        expect(result.current.order).toEqual("DESC");
        expect(result.current.count).toEqual(10);
    });
});
