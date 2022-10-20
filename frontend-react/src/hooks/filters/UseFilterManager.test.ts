import { renderHook } from "@testing-library/react-hooks";

import useFilterManager, {
    cursorOrRange,
    extractFiltersFromManager,
    FilterManager,
} from "./UseFilterManager";
import { RangeField, RangeSettings } from "./UseDateRange";
import { PageSettings } from "./UsePages";
import { SortSettings } from "./UseSortOrder";

const mockUpdateSort = jest.fn();
const mockUpdatePage = jest.fn();
const mockUpdateRange = jest.fn();
const mockResetAll = jest.fn();
const mockSortSettings: SortSettings = {
    column: "",
    order: "DESC",
    locally: false,
    localOrder: "DESC",
};
const mockPageSettings: PageSettings = {
    size: 10,
    currentPage: 0,
};
const mockRangeSettings: RangeSettings = {
    to: "",
    from: "",
};

const TEST_FILTER_MANAGER: FilterManager = {
    sortSettings: mockSortSettings,
    pageSettings: mockPageSettings,
    rangeSettings: mockRangeSettings,
    updateSort: mockUpdateSort,
    updatePage: mockUpdatePage,
    updateRange: mockUpdateRange,
    resetAll: mockResetAll,
};

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

describe("extractFiltersFromManager", () => {
    test("gives only filters back", () => {
        const filters = extractFiltersFromManager(TEST_FILTER_MANAGER);
        const {
            to,
            from,
            currentPage,
            size,
            column,
            locally,
            order,
            localOrder,
        } = filters;
        expect({ to, from }).toEqual(mockRangeSettings);
        expect({ currentPage, size }).toEqual(mockPageSettings);
        expect({ column, locally, order, localOrder }).toEqual(
            mockSortSettings
        );
    });
});
