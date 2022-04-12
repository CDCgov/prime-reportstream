import { act, renderHook } from "@testing-library/react-hooks";

import useSortOrder from "./UseSortOrder";

describe("UseSortOrder", () => {
    test("renders with friendly default values", () => {
        const { result } = renderHook(() => useSortOrder());
        expect(result.current.column).toEqual("");
        expect(result.current.order).toEqual("DESC");
    });

    test("setting SortOrder", () => {
        const { result } = renderHook(() => useSortOrder());

        act(() => result.current.setSort("test", "ASC"));

        expect(result.current.column).toEqual("test");
        expect(result.current.order).toEqual("DESC");
    });
});
