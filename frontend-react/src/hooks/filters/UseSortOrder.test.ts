import { act, renderHook } from "@testing-library/react-hooks";

import useSortOrder from "./UseSortOrder";

describe("UseDateRange", () => {
    test("renders with friendly default values", () => {
        const { result } = renderHook(() => useSortOrder());
        expect(result.current.column).toEqual("");
        expect(result.current.order).toEqual("DESC");
    });

    test("selecting column updates column value", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() => result.current.set("test"));
        expect(result.current.column).toEqual("test");
        expect(result.current.order).toEqual("ASC");
    });

    test("selecting order updates order value", () => {
        const { result } = renderHook(() => useSortOrder());
        // Simulates selecting a column then swapping order
        act(() => result.current.set("test"));
        act(() => result.current.set("test"));

        expect(result.current.column).toEqual("test");
        expect(result.current.order).toEqual("DESC");
    });
});
