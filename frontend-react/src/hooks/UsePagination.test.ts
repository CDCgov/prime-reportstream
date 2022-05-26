import { act, renderHook } from "@testing-library/react-hooks";
import range from "lodash.range";

import { OVERFLOW_INDICATOR } from "../components/Table/Pagination";

import usePagination, {
    CursorExtractor,
    getSlots,
    PaginationState,
    UsePaginationProps,
    setCurrentPageReducer,
    processResultsReducer,
} from "./UsePagination";

interface SampleRecord {
    cursor: string;
}
const extractCursor: CursorExtractor<SampleRecord> = (r) => r.cursor;

function createSampleRecords(
    numRecords: number,
    startCursor = 1
): SampleRecord[] {
    return range(startCursor, startCursor + numRecords).map(
        (c) => ({ cursor: c.toString() } as SampleRecord)
    );
}

describe("getSlots", () => {
    test("when the set is unbounded", () => {
        expect(getSlots(1)).toStrictEqual([
            1,
            2,
            3,
            4,
            5,
            6,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(2)).toStrictEqual([
            1,
            2,
            3,
            4,
            5,
            6,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(3)).toStrictEqual([
            1,
            2,
            3,
            4,
            5,
            6,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(4)).toStrictEqual([
            1,
            2,
            3,
            4,
            5,
            6,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(5)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            4,
            5,
            6,
            7,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(20)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            19,
            20,
            21,
            22,
            OVERFLOW_INDICATOR,
        ]);
    });

    test("when the set is bounded", () => {
        expect(getSlots(2, 8)).toStrictEqual([
            1,
            2,
            3,
            4,
            5,
            OVERFLOW_INDICATOR,
            8,
        ]);
        expect(getSlots(4, 10)).toStrictEqual([
            1,
            2,
            3,
            4,
            5,
            OVERFLOW_INDICATOR,
            10,
        ]);
        expect(getSlots(6, 10)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            5,
            6,
            7,
            OVERFLOW_INDICATOR,
            10,
        ]);
        expect(getSlots(7, 10)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
            8,
            9,
            10,
        ]);
        expect(getSlots(8, 10)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
            8,
            9,
            10,
        ]);
        expect(getSlots(9, 10)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
            8,
            9,
            10,
        ]);
        expect(getSlots(10, 10)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
            8,
            9,
            10,
        ]);
    });

    test("when there are no more than seven pages", () => {
        expect(getSlots(1, 1)).toStrictEqual([1]);

        expect(getSlots(1, 2)).toStrictEqual([1, 2]);
        expect(getSlots(2, 2)).toStrictEqual([1, 2]);

        expect(getSlots(1, 3)).toStrictEqual([1, 2, 3]);
        expect(getSlots(2, 3)).toStrictEqual([1, 2, 3]);
        expect(getSlots(3, 3)).toStrictEqual([1, 2, 3]);

        expect(getSlots(1, 7)).toStrictEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(getSlots(2, 7)).toStrictEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(getSlots(3, 7)).toStrictEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(getSlots(4, 7)).toStrictEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(getSlots(5, 7)).toStrictEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(getSlots(6, 7)).toStrictEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(getSlots(7, 7)).toStrictEqual([1, 2, 3, 4, 5, 6, 7]);
    });
});

describe("processResultsReducer", () => {
    test("with all of the requested results", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 1,
            extractCursor,
            pageSize: 10,
            pageCursorMap: { 1: "0" },
            pageResultsMap: {},
            fetchStartPageNum: 1,
            fetchNumResults: 61,
            fetchStartCursor: "0",
        };
        const results = createSampleRecords(61);
        expect(processResultsReducer(state, results)).toStrictEqual({
            ...state,
            pageResultsMap: {
                1: results.slice(0, 10),
                2: results.slice(10, 20),
                3: results.slice(20, 30),
                4: results.slice(30, 40),
                5: results.slice(40, 50),
                6: results.slice(50, 60),
            },
            pageCursorMap: {
                1: "0",
                2: "11",
                3: "21",
                4: "31",
                5: "41",
                6: "51",
                7: "61",
            },
            finalPageNum: undefined,
        });
    });

    test("with fewer than the requested number of results", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 1,
            extractCursor,
            pageSize: 10,
            pageCursorMap: { 1: "0" },
            pageResultsMap: {},
            fetchStartPageNum: 1,
            fetchNumResults: 61,
            fetchStartCursor: "0",
        };
        const results = createSampleRecords(30);
        expect(processResultsReducer(state, results)).toStrictEqual({
            ...state,
            pageResultsMap: {
                1: results.slice(0, 10),
                2: results.slice(10, 20),
                3: results.slice(20),
            },
            pageCursorMap: {
                1: "0",
                2: "11",
                3: "21",
            },
            finalPageNum: 3,
        });
    });
});

describe("setCurrentPageReducer", () => {
    test("with no stored data", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 1,
            extractCursor,
            pageSize: 10,
            pageCursorMap: { 1: "0" },
            pageResultsMap: {},
            fetchStartPageNum: 1,
            fetchNumResults: 61,
            fetchStartCursor: "0",
        };
        expect(setCurrentPageReducer(state, 1)).toStrictEqual({
            ...state,
            currentPageNum: 1,
            fetchNumResults: 61,
            fetchStartCursor: "0",
            fetchStartPageNum: 1,
        });
    });

    test("when some of the data has been fetched", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 1,
            extractCursor,
            pageSize: 10,
            pageCursorMap: {
                1: "0",
                2: "11",
                3: "21",
                4: "31",
                5: "41",
                6: "51",
                7: "61",
            },
            pageResultsMap: { 1: [], 2: [], 3: [], 4: [], 5: [], 6: [] },
        };
        expect(setCurrentPageReducer(state, 6)).toStrictEqual({
            ...state,
            currentPageNum: 6,
            fetchNumResults: 21,
            fetchStartCursor: "61",
            fetchStartPageNum: 7,
        });
    });

    test("when all of the data has been fetched", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 7,
            extractCursor,
            pageSize: 10,
            pageCursorMap: {
                1: "0",
                2: "11",
                3: "21",
                4: "31",
                5: "41",
                6: "51",
                7: "61",
                8: "71",
                9: "81",
                10: "91",
            },
            pageResultsMap: {
                1: [],
                2: [],
                3: [],
                4: [],
                5: [],
                6: [],
                7: [],
                8: [],
                9: [],
                10: [],
            },
            finalPageNum: 10,
        };
        expect(setCurrentPageReducer(state, 10)).toStrictEqual({
            ...state,
            currentPageNum: 10,
        });
    });
});

describe("usePagination", () => {
    function doRenderHook(initialProps: UsePaginationProps<SampleRecord>) {
        return renderHook(
            (props: UsePaginationProps<SampleRecord>) =>
                usePagination<SampleRecord>(props),
            {
                initialProps,
            }
        );
    }

    test("Returns empty pagination props when there are no results", async () => {
        const mockFetchResults = jest.fn().mockResolvedValueOnce([]);
        const { result, waitForNextUpdate } = doRenderHook({
            startCursor: "0",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        await waitForNextUpdate();
        // The request on the first page should check for the presence of up to seven pages.
        expect(mockFetchResults).toHaveBeenLastCalledWith("0", 61);
        expect(result.current.paginationProps).toBeUndefined();
        expect(result.current.currentPageResults).toStrictEqual([]);
    });

    test("Fetches results and updates the available slots and page of results", async () => {
        const results = createSampleRecords(40);
        const mockFetchResults = jest.fn().mockResolvedValueOnce(results);
        const { result, waitForNextUpdate } = doRenderHook({
            startCursor: "0",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        await waitForNextUpdate();
        // The request on the first page should check for the presence of up to seven pages.
        expect(mockFetchResults).toHaveBeenLastCalledWith("0", 61);
        expect(result.current.paginationProps?.currentPageNum).toBe(1);
        expect(result.current.paginationProps?.slots).toStrictEqual([
            1, 2, 3, 4,
        ]);
        expect(result.current.currentPageResults).toStrictEqual(
            results.slice(0, 10)
        );
    });

    test("Setting a page fetches a new batch of results and updates the state", async () => {
        const results1 = createSampleRecords(61);
        const results2 = createSampleRecords(21, 61);
        const mockFetchResults = jest
            .fn()
            .mockResolvedValueOnce(results1)
            .mockResolvedValueOnce(results2);
        const { result, waitForNextUpdate } = doRenderHook({
            startCursor: "0",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        await waitForNextUpdate();
        act(() => {
            result.current.paginationProps?.setCurrentPage(6);
        });
        await waitForNextUpdate();
        expect(mockFetchResults).toHaveBeenLastCalledWith("61", 21);
        expect(result.current.paginationProps?.currentPageNum).toBe(6);
        expect(result.current.paginationProps?.slots).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            5,
            6,
            7,
            8,
            OVERFLOW_INDICATOR,
        ]);

        // The current page of results is still from the first fetch. The second fetch was needed to
        // extend the pagination, not get the results for the current page.
        expect(result.current.currentPageResults).toStrictEqual(
            results1.slice(50, 60)
        );
    });

    test("Changing the initial start cursor resets the state", async () => {
        const mockFetchResults = jest
            .fn()
            .mockResolvedValueOnce(createSampleRecords(11))
            .mockResolvedValueOnce(createSampleRecords(11));
        const { result, rerender, waitForNextUpdate } = doRenderHook({
            startCursor: "0",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });

        // Wait for the fetch promise to resolve, then check the slots and move to the next page.
        await waitForNextUpdate();
        expect(mockFetchResults).toHaveBeenLastCalledWith("0", 61);
        expect(result.current.paginationProps?.slots).toStrictEqual([1, 2]);
        act(() => {
            result.current.paginationProps?.setCurrentPage(2);
        });
        expect(result.current.paginationProps?.currentPageNum).toBe(2);

        // Rerender with a new start cursor.
        rerender({
            startCursor: "9999",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        await waitForNextUpdate();
        // After a reset, the fetch count should reflect an initial request, which needs to check
        // for the presence of up to five pages.
        expect(mockFetchResults).toHaveBeenLastCalledWith("9999", 61);
        expect(result.current.paginationProps?.currentPageNum).toBe(1);
    });

    test("Changing the fetchResults function resets the state", async () => {
        const mockFetchResults1 = jest
            .fn()
            .mockResolvedValueOnce(createSampleRecords(11))
            .mockResolvedValueOnce(createSampleRecords(1, 11));
        const mockFetchResults2 = jest
            .fn()
            .mockResolvedValueOnce(createSampleRecords(1));
        const initialProps = {
            startCursor: "1",
            pageSize: 10,
            fetchResults: mockFetchResults1,
            extractCursor,
        };
        const { result, rerender, waitForNextUpdate } =
            doRenderHook(initialProps);

        // Set the results and move to the second page.
        await waitForNextUpdate();
        act(() => {
            result.current.paginationProps?.setCurrentPage(2);
        });
        expect(result.current.paginationProps?.slots).toStrictEqual([1, 2]);
        expect(result.current.paginationProps?.currentPageNum).toBe(2);

        // Rerender with a new fetch results callback to create the list request parameters, e.g. when the sort
        // order changes.
        rerender({
            ...initialProps,
            fetchResults: mockFetchResults2,
        });
        await waitForNextUpdate();
        // The initial request should check for the presence of up to five pages.
        expect(mockFetchResults2).toHaveBeenLastCalledWith("1", 61);
        expect(result.current.paginationProps?.currentPageNum).toBe(1);
    });
});
