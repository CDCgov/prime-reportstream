import { act, renderHook } from "@testing-library/react-hooks";
import range from "lodash.range";

import usePagination, {
    CursorExtractor,
    getSlotsForResultSet,
    OVERFLOW_INDICATOR,
    PaginationActionType,
} from "./UsePagination";

describe("usePagination", () => {
    interface SampleRecord {
        cursor: string;
    }
    const cursorExtractor: CursorExtractor<SampleRecord> = (r) => r.cursor;

    function createSampleRecords(numRecords: number): SampleRecord[] {
        return range(1, numRecords + 1).map(
            (c) => ({ cursor: c.toString() } as SampleRecord)
        );
    }

    test("Hook renders with initial values", () => {
        const { result } = renderHook(() =>
            usePagination<SampleRecord>({
                startCursor: "1",
                pageSize: 10,
                cursorExtractor,
            })
        );

        expect(result.current.state.fetchStartCursor).toBe("1");
        // The initial request should check for the presence of up to five pages.
        expect(result.current.state.fetchCount).toBe(41);
        expect(result.current.state.currentPageNum).toBe(1);
        expect(result.current.state.slots).toStrictEqual([]);
    });

    test("Providing a set of results updates slots", () => {
        const { result } = renderHook(() =>
            usePagination<SampleRecord>({
                startCursor: "1",
                pageSize: 10,
                cursorExtractor,
            })
        );

        act(() => {
            result.current.dispatch({
                type: PaginationActionType.SET_RESULTS,
                payload: createSampleRecords(10),
            });
        });
        expect(result.current.state.slots).toStrictEqual([1]);
    });

    test("Setting a page updates the fetch range and num results", () => {
        const { result } = renderHook(() =>
            usePagination<SampleRecord>({
                startCursor: "1",
                pageSize: 10,
                cursorExtractor,
            })
        );

        act(() => {
            result.current.dispatch({
                type: PaginationActionType.SET_RESULTS,
                payload: createSampleRecords(41),
            });
        });
        expect(result.current.state.slots).toStrictEqual([
            1,
            2,
            3,
            4,
            OVERFLOW_INDICATOR,
        ]);

        act(() => {
            result.current.dispatch({
                type: PaginationActionType.SET_CURRENT_PAGE,
                payload: 4,
            });
        });
        // Expect to now fetch starting from the cursor of the first item on the fourth page.
        expect(result.current.state.fetchStartCursor).toBe("31");
        expect(result.current.state.fetchCount).toBe(31);
    });

    test("Dispatching a RESET action resets the state", () => {
        const { result } = renderHook(() =>
            usePagination<SampleRecord>({
                startCursor: "1",
                pageSize: 10,
                cursorExtractor,
            })
        );

        // Set the results and move to the second page.
        act(() => {
            result.current.dispatch({
                type: PaginationActionType.SET_RESULTS,
                payload: createSampleRecords(11),
            });
        });
        act(() => {
            result.current.dispatch({
                type: PaginationActionType.SET_CURRENT_PAGE,
                payload: 2,
            });
        });
        expect(result.current.state.slots).toStrictEqual([1, 2]);
        expect(result.current.state.currentPageNum).toBe(2);

        // Reset the hook state, for example, due to changing the sort order.
        act(() => {
            result.current.dispatch({
                type: PaginationActionType.RESET,
                payload: {
                    startCursor: "9999",
                    pageSize: 10,
                    cursorExtractor,
                },
            });
        });
        expect(result.current.state.fetchStartCursor).toStrictEqual("9999");
        // The initial request should check for the presence of up to five pages.
        expect(result.current.state.fetchCount).toBe(41);
        expect(result.current.state.currentPageNum).toBe(1);
    });
});

describe("getSlotsForResultSet", () => {
    test("first three pages", () => {
        expect(getSlotsForResultSet<number>(range(10), 10, 1)).toStrictEqual([
            1,
        ]);
        expect(getSlotsForResultSet<number>(range(15), 10, 1)).toStrictEqual([
            1, 2,
        ]);
        expect(getSlotsForResultSet<number>(range(21), 10, 1)).toStrictEqual([
            1, 2, 3,
        ]);
        expect(getSlotsForResultSet<number>(range(41), 10, 1)).toStrictEqual([
            1,
            2,
            3,
            4,
            OVERFLOW_INDICATOR,
        ]);
        expect(getSlotsForResultSet<number>(range(100), 10, 1)).toStrictEqual([
            1,
            2,
            3,
            4,
            OVERFLOW_INDICATOR,
        ]);
    });

    test("subsequent pages", () => {
        // There shouldn't be a gap between 1 and 3 when there are only four pages
        expect(getSlotsForResultSet<number>(range(10), 10, 4)).toStrictEqual([
            1, 2, 3, 4,
        ]);

        expect(getSlotsForResultSet<number>(range(20), 10, 4)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            3,
            4,
            5,
        ]);

        expect(getSlotsForResultSet<number>(range(31), 10, 4)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            3,
            4,
            5,
            6,
            OVERFLOW_INDICATOR,
        ]);

        expect(getSlotsForResultSet<number>(range(10), 10, 7)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
        ]);

        expect(getSlotsForResultSet<number>(range(20), 10, 7)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
            8,
        ]);

        expect(getSlotsForResultSet<number>(range(31), 10, 7)).toStrictEqual([
            1,
            OVERFLOW_INDICATOR,
            6,
            7,
            8,
            9,
            OVERFLOW_INDICATOR,
        ]);
    });
});
