import { act, renderHook } from "@testing-library/react-hooks";

import usePageSize from "./UsePageSize";

describe("UsePageSize", () => {
    test("defaults to page size of 10", () => {
        const { result } = renderHook(() => usePageSize());
        expect(result.current.count).toEqual(10);
    });
    test("setter updates size", () => {
        const { result } = renderHook(() => usePageSize());
        act(() => result.current.set(25));
        expect(result.current.count).toEqual(25);
    });
});
