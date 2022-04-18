import { renderHook } from "@testing-library/react-hooks";

import useFilterManager from "./UseFilterManager";

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
