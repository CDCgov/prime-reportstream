import { act, renderHook, waitFor } from "@testing-library/react";
import range from "lodash.range";

import { mockAppInsights } from "../utils/__mocks__/ApplicationInsights";
import { OVERFLOW_INDICATOR } from "../components/Table/Pagination";

import usePagination, {
    CursorExtractor,
    getSlots,
    PaginationState,
    ProcessResultsPayload,
    UsePaginationProps,
    setSelectedPageReducer,
    processResultsReducer,
} from "./UsePagination";

interface SampleRecord {
    cursor: string;
}
const extractCursor: CursorExtractor<SampleRecord> = (r) => r.cursor;

jest.mock("../TelemetryService", () => ({
    ...jest.requireActual("../TelemetryService"),
    getAppInsights: () => mockAppInsights,
}));

function createSampleRecords(
    numRecords: number,
    startCursor = 1,
): SampleRecord[] {
    return range(startCursor, startCursor + numRecords).map(
        (c) => ({ cursor: c.toString() }) as SampleRecord,
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
    function getInitialState(): PaginationState<SampleRecord> {
        return {
            currentPageNum: 1,
            extractCursor,
            isCursorInclusive: false,
            isLoading: true,
            pageSize: 10,
            pageCursorMap: { 1: "0" },
            pageResultsMap: {},
        };
    }

    test("with all of the requested results", () => {
        const state: PaginationState<SampleRecord> = getInitialState();
        const results = createSampleRecords(61);
        const payload: ProcessResultsPayload<SampleRecord> = {
            results,
            requestConfig: {
                selectedPageNum: 1,
                cursorPageNum: 1,
                numResults: 61,
                cursor: "0",
            },
        };
        expect(processResultsReducer(state, payload)).toStrictEqual({
            ...state,
            isLoading: false,
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
                2: "10",
                3: "20",
                4: "30",
                5: "40",
                6: "50",
                7: "60",
            },
            finalPageNum: undefined,
        });
    });

    test("with fewer than the requested number of results", () => {
        const state: PaginationState<SampleRecord> = getInitialState();
        const results = createSampleRecords(30);
        const payload: ProcessResultsPayload<SampleRecord> = {
            results,
            requestConfig: {
                selectedPageNum: 1,
                cursorPageNum: 1,
                numResults: 61,
                cursor: "0",
            },
        };
        expect(processResultsReducer(state, payload)).toStrictEqual({
            ...state,
            isLoading: false,
            pageResultsMap: {
                1: results.slice(0, 10),
                2: results.slice(10, 20),
                3: results.slice(20),
            },
            pageCursorMap: {
                1: "0",
                2: "10",
                3: "20",
            },
            finalPageNum: 3,
        });
    });

    test("stores inclusive cursors from the first result on the page", () => {
        const state: PaginationState<SampleRecord> = {
            ...getInitialState(),
            isCursorInclusive: true,
        };
        const results = createSampleRecords(61);
        const payload: ProcessResultsPayload<SampleRecord> = {
            results,
            requestConfig: {
                selectedPageNum: 1,
                cursorPageNum: 1,
                numResults: 61,
                cursor: "0",
            },
        };
        expect(processResultsReducer(state, payload)).toStrictEqual({
            ...state,
            isLoading: false,
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
});

describe("setSelectedPageReducer", () => {
    test("with no stored data", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 1,
            extractCursor,
            isCursorInclusive: false,
            isLoading: false,
            pageSize: 10,
            pageCursorMap: { 1: "0" },
            pageResultsMap: {},
        };
        expect(setSelectedPageReducer(state, 1)).toStrictEqual({
            ...state,
            isLoading: true,
            requestConfig: {
                numResults: 61,
                cursor: "0",
                cursorPageNum: 1,
                selectedPageNum: 1,
            },
        } as PaginationState<SampleRecord>);
    });

    test("when some of the data has been fetched", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 1,
            extractCursor,
            isCursorInclusive: false,
            isLoading: false,
            pageSize: 10,
            pageCursorMap: {
                1: "0",
                2: "10",
                3: "20",
                4: "30",
                5: "40",
                6: "50",
                7: "60",
            },
            pageResultsMap: { 1: [], 2: [], 3: [], 4: [], 5: [], 6: [] },
        };
        expect(setSelectedPageReducer(state, 6)).toStrictEqual({
            ...state,
            isLoading: true,
            requestConfig: {
                numResults: 21,
                cursor: "60",
                cursorPageNum: 7,
                selectedPageNum: 6,
            },
        } as PaginationState<SampleRecord>);
    });

    test("when going to a previous page", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 5,
            extractCursor,
            isCursorInclusive: false,
            isLoading: false,
            pageSize: 10,
            pageCursorMap: {
                1: "0",
                2: "10",
                3: "20",
                4: "30",
                5: "40",
                6: "50",
                7: "60",
                8: "70",
            },
            pageResultsMap: { 1: [], 2: [], 3: [], 4: [], 5: [], 6: [], 7: [] },
        };
        expect(setSelectedPageReducer(state, 4)).toStrictEqual({
            ...state,
            currentPageNum: 4,
            requestConfig: undefined,
        } as PaginationState<SampleRecord>);
    });

    test("when all of the data has been fetched", () => {
        const state: PaginationState<SampleRecord> = {
            currentPageNum: 7,
            extractCursor,
            isCursorInclusive: false,
            isLoading: false,
            pageSize: 10,
            pageCursorMap: {
                1: "0",
                2: "10",
                3: "20",
                4: "30",
                5: "40",
                6: "50",
                7: "60",
                8: "70",
                9: "80",
                10: "90",
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
        expect(setSelectedPageReducer(state, 10)).toStrictEqual({
            ...state,
            currentPageNum: 10,
            requestConfig: undefined,
        } as PaginationState<SampleRecord>);
    });
});

describe("usePagination", () => {
    function doRenderHook(initialProps: UsePaginationProps<SampleRecord>) {
        return renderHook(
            (props: UsePaginationProps<SampleRecord>) =>
                usePagination<SampleRecord>(props),
            {
                initialProps,
            },
        );
    }

    test("Returns empty pagination props when there are no results", async () => {
        const mockFetchResults = jest.fn().mockResolvedValueOnce([]);
        const { result } = doRenderHook({
            startCursor: "0",
            isCursorInclusive: false,
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        // The request on the first page should check for the presence of up to
        // seven pages.
        await (() =>
            expect(mockFetchResults).toHaveBeenLastCalledWith("0", 61));
        expect(result.current.paginationProps).toBeUndefined();
        expect(result.current.currentPageResults).toStrictEqual([]);
    });

    test("Fetches results and updates the available slots and page of results", async () => {
        const results = createSampleRecords(40);
        const mockFetchResults = jest.fn().mockResolvedValueOnce(results);
        const { result } = doRenderHook({
            startCursor: "0",
            isCursorInclusive: false,
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        // The request on the first page should check for the presence of up to
        // seven pages.
        await waitFor(() =>
            expect(mockFetchResults).toHaveBeenLastCalledWith("0", 61),
        );
        expect(result.current.paginationProps?.currentPageNum).toBe(1);
        expect(result.current.paginationProps?.slots).toStrictEqual([
            1, 2, 3, 4,
        ]);
        expect(result.current.currentPageResults).toStrictEqual(
            results.slice(0, 10),
        );
    });

    test("Setting a page fetches a new batch of results and updates the state", async () => {
        const results1 = createSampleRecords(61);
        const results2 = createSampleRecords(21, 61);
        const mockFetchResults = jest
            .fn()
            .mockResolvedValueOnce(results1)
            .mockResolvedValueOnce(results2);
        const { result } = doRenderHook({
            startCursor: "0",
            isCursorInclusive: false,
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        await waitFor(() =>
            expect(result.current.paginationProps).toBeDefined(),
        );
        act(() => {
            result.current.paginationProps?.setSelectedPage(6);
        });
        // The current page and slots should not update until the fetch resolves
        expect(result.current.paginationProps?.currentPageNum).toBe(1);
        expect(result.current.paginationProps?.slots).toStrictEqual([
            1,
            2,
            3,
            4,
            5,
            6,
            OVERFLOW_INDICATOR,
        ]);
        expect(result.current.isLoading).toBe(true);

        await waitFor(() =>
            expect(
                result.current.paginationProps?.currentPageNum,
            ).toBeGreaterThan(1),
        );
        expect(mockFetchResults).toHaveBeenLastCalledWith("60", 21);
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
        expect(result.current.isLoading).toBe(false);

        // The current page of results is still from the first fetch. The second
        // fetch was needed to extend the pagination, not get the results for
        // the current page.
        expect(result.current.currentPageResults).toStrictEqual(
            results1.slice(50, 60),
        );
    });

    test("Changing the initial start cursor resets the state", async () => {
        const mockFetchResults = jest
            .fn()
            .mockResolvedValueOnce(createSampleRecords(11))
            .mockResolvedValueOnce(createSampleRecords(11));
        const { result, rerender } = doRenderHook({
            startCursor: "0",
            isCursorInclusive: false,
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });

        // Wait for the fetch promise to resolve, then check the slots and move
        // to the next page.
        await waitFor(() =>
            expect(result.current.paginationProps).toBeDefined(),
        );
        expect(mockFetchResults).toHaveBeenLastCalledWith("0", 61);
        expect(result.current.paginationProps?.slots).toStrictEqual([1, 2]);
        act(() => {
            result.current.paginationProps?.setSelectedPage(2);
        });
        // The current page should update right away since we don't need to
        // fetch any more results.
        expect(result.current.paginationProps?.currentPageNum).toBe(2);

        // Rerender with a new start cursor.
        rerender({
            startCursor: "9999",
            isCursorInclusive: false,
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        await waitFor(() =>
            expect(result.current.paginationProps).toBeDefined(),
        );
        // After a reset, the fetch count should reflect an initial request,
        // which needs to check for the presence of up to five pages.
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
            isCursorInclusive: false,
            pageSize: 10,
            fetchResults: mockFetchResults1,
            extractCursor,
        };
        const { result, rerender } = doRenderHook(initialProps);

        // Set the results and move to the second page.
        await waitFor(() =>
            expect(result.current.paginationProps).toBeDefined(),
        );
        act(() => {
            result.current.paginationProps?.setSelectedPage(2);
        });
        expect(result.current.paginationProps?.slots).toStrictEqual([1, 2]);
        expect(result.current.paginationProps?.currentPageNum).toBe(2);

        // Rerender with a new fetch results callback to create the list request
        // parameters, e.g. when the sort order changes.
        rerender({
            ...initialProps,
            fetchResults: mockFetchResults2,
        });
        await waitFor(() =>
            expect(
                result.current.paginationProps?.currentPageNum,
            ).toBeDefined(),
        );
        // The initial request should check for the presence of up to five pages.
        expect(mockFetchResults2).toHaveBeenLastCalledWith("1", 61);
        expect(result.current.paginationProps?.currentPageNum).toBe(1);
    });

    test("Calls appInsights.trackEvent with page size and page number.", async () => {
        const mockFetchResults = jest
            .fn()
            .mockResolvedValueOnce(createSampleRecords(11))
            .mockResolvedValueOnce(createSampleRecords(11));
        const { result } = doRenderHook({
            startCursor: "0",
            isCursorInclusive: false,
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
            analyticsEventName: "Test Analytics Event",
        });

        // Wait for the fetch promise to resolve, then check the slots and move
        // to the next page.
        await waitFor(() =>
            expect(result.current.paginationProps).toBeDefined(),
        );
        expect(mockFetchResults).toHaveBeenLastCalledWith("0", 61);
        expect(result.current.paginationProps?.slots).toStrictEqual([1, 2]);
        expect(mockAppInsights.trackEvent).not.toBeCalled();

        act(() => {
            result.current.paginationProps?.setSelectedPage(2);
        });

        expect(mockAppInsights.trackEvent).toBeCalledWith({
            name: "Test Analytics Event",
            properties: {
                tablePagination: {
                    pageSize: 10,
                    pageNumber: 2,
                },
            },
        });
    });
});
