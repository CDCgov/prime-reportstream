import { act, renderHook } from "@testing-library/react-hooks";
import range from "lodash.range";

import { OVERFLOW_INDICATOR } from "../components/Table/Pagination";

import usePagination, {
    CursorExtractor,
    getSlotsForResultSet,
    UsePaginationProps,
} from "./UsePagination";

describe("usePagination", () => {
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

    function doRenderHook(initialProps: UsePaginationProps<SampleRecord>) {
        return renderHook(
            (props: UsePaginationProps<SampleRecord>) =>
                usePagination<SampleRecord>(props),
            {
                initialProps,
            }
        );
    }

    test("Fetches results and updates the available slots and page of results", async () => {
        const results = createSampleRecords(41);
        const mockFetchResults = jest.fn().mockResolvedValueOnce(results);
        const { result, waitForNextUpdate } = doRenderHook({
            startCursor: "1",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });
        await waitForNextUpdate();
        // The request on the first page should check for the presence of up to five pages.
        expect(mockFetchResults).toHaveBeenLastCalledWith("1", 41);
        expect(result.current.paginationProps.currentPageNum).toBe(1);
        expect(result.current.paginationProps.slots).toStrictEqual([
            1,
            2,
            3,
            4,
            OVERFLOW_INDICATOR,
        ]);
        expect(result.current.resultsPage).toStrictEqual(results.slice(0, 10));
    });

    test("Setting a page fetches a new batch of results and updates the state", async () => {
        const results1 = createSampleRecords(41);
        const results2 = createSampleRecords(31, 31);
        const mockFetchResults = jest
            .fn()
            .mockResolvedValueOnce(results1)
            .mockResolvedValueOnce(results2);
        const { result, waitForNextUpdate } = doRenderHook({
            startCursor: "1",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });

        await waitForNextUpdate();
        act(() => {
            result.current.paginationProps.setCurrentPage(2);
        });
        await waitForNextUpdate();
        expect(mockFetchResults).toHaveBeenLastCalledWith("11", 31);
        expect(result.current.paginationProps.currentPageNum).toBe(2);
        expect(result.current.paginationProps.slots).toStrictEqual([
            1,
            2,
            3,
            4,
            OVERFLOW_INDICATOR,
        ]);
        expect(result.current.resultsPage).toStrictEqual(results2.slice(0, 10));
    });

    test("Changing the initial start cursor resets the state", async () => {
        const mockFetchResults = jest
            .fn()
            .mockResolvedValueOnce(createSampleRecords(11));
        const { result, rerender, waitForNextUpdate } = doRenderHook({
            startCursor: "1",
            pageSize: 10,
            fetchResults: mockFetchResults,
            extractCursor,
        });

        // Wait for the fetch promise to resolve, then check the slots and move to the next page.
        await waitForNextUpdate();
        expect(mockFetchResults).toHaveBeenLastCalledWith("1", 41);
        expect(result.current.paginationProps.slots).toStrictEqual([1, 2]);
        act(() => {
            result.current.paginationProps.setCurrentPage(2);
        });
        expect(result.current.paginationProps.currentPageNum).toBe(2);

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
        expect(mockFetchResults).toHaveBeenLastCalledWith("9999", 41);
        expect(result.current.paginationProps.currentPageNum).toBe(1);
    });

    test("Changing the createListParams function resets the state", async () => {
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
            result.current.paginationProps.setCurrentPage(2);
        });
        await waitForNextUpdate();
        expect(mockFetchResults1).toHaveBeenLastCalledWith("11", 31);
        expect(result.current.paginationProps.slots).toStrictEqual([1, 2]);
        expect(result.current.paginationProps.currentPageNum).toBe(2);

        // Rerender with a new fetch results callback to create the list request parameters, e.g. when the sort
        // order changes.
        rerender({
            ...initialProps,
            fetchResults: mockFetchResults2,
        });
        await waitForNextUpdate();
        // The initial request should check for the presence of up to five pages.
        expect(mockFetchResults2).toHaveBeenLastCalledWith("1", 41);
        expect(result.current.paginationProps.currentPageNum).toBe(1);
    });
});

describe("getSlotsForResultSet", () => {
    // Helper function to call the code under test, assuming 10 items per page.
    function getSlots(numResults: number, firstPageInResultSet: number) {
        return getSlotsForResultSet<number>(
            range(numResults),
            10,
            firstPageInResultSet
        );
    }

    test("when there are 10 pages of results", () => {
        expect(getSlots(41, 1)).toStrictEqual([1, 2, 3, 4, OVERFLOW_INDICATOR]);
        expect(getSlots(31, 2)).toStrictEqual([1, 2, 3, 4, OVERFLOW_INDICATOR]);
        expect(getSlots(31, 3)).toStrictEqual([1, 2, 3, 4, OVERFLOW_INDICATOR]);
        expect(getSlots(31, 4)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            3,
            4,
            5,
            6,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(31, 5)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            4,
            5,
            6,
            7,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(31, 6)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            5,
            6,
            7,
            8,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(31, 7)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
            8,
            9,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlots(30, 8)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            7,
            8,
            9,
            10,
        ]);
    });

    test("when there are no more than four pages", () => {
        expect(getSlots(10, 1)).toStrictEqual([1]);

        expect(getSlots(15, 1)).toStrictEqual([1, 2]);
        expect(getSlots(5, 2)).toStrictEqual([1, 2]);

        expect(getSlots(21, 1)).toStrictEqual([1, 2, 3]);
        expect(getSlots(11, 2)).toStrictEqual([1, 2, 3]);
        expect(getSlots(1, 3)).toStrictEqual([1, 2, 3]);

        expect(getSlots(40, 1)).toStrictEqual([1, 2, 3, 4]);
        expect(getSlots(30, 2)).toStrictEqual([1, 2, 3, 4]);
        expect(getSlots(20, 3)).toStrictEqual([1, 2, 3, 4]);
        expect(getSlots(10, 4)).toStrictEqual([1, 2, 3, 4]);
    });

    test("when there a previous overflow indicator but not a next one", () => {
        expect(getSlots(20, 4)).toStrictEqual([1, OVERFLOW_INDICATOR, 3, 4, 5]);
        expect(getSlots(10, 7)).toStrictEqual([1, OVERFLOW_INDICATOR, 6, 7]);
        expect(getSlots(20, 7)).toStrictEqual([1, OVERFLOW_INDICATOR, 6, 7, 8]);
    });
});
