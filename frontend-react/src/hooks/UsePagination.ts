import React, { useReducer } from "react";
import chunk from "lodash.chunk";
import range from "lodash.range";

export const OVERFLOW_INDICATOR = Symbol("...");
export type SlotItem = number | typeof OVERFLOW_INDICATOR;

// Helper function to determine the slots for the pagination component based on the current state
// and a new set of results.
export function getSlotsForResultSet<T>(
    results: T[],
    pageSize: number,
    firstPageInResultSet: number
): SlotItem[] {
    const numResultPages = chunk(results, pageSize).length;
    if (firstPageInResultSet < 4) {
        if (numResultPages > 4) {
            return [1, 2, 3, 4, OVERFLOW_INDICATOR];
        }
        return range(1, numResultPages + 1);
    } else {
        if (firstPageInResultSet === 4 && numResultPages === 1) {
            return [1, 2, 3, 4];
        }

        const slots: SlotItem[] = [1, OVERFLOW_INDICATOR];
        // After the first four pages, we show the previous page and two subsequent pages.
        // Add the previous page
        slots.push(firstPageInResultSet - 1);

        if (numResultPages > 3) {
            // Add slots for the first page in the result set, the two after it, and an overflow.
            slots.push(
                ...range(firstPageInResultSet, firstPageInResultSet + 3),
                OVERFLOW_INDICATOR
            );
        } else {
            // Add slots for the first page in the result set and the fewer-than-three pages after.
            slots.push(
                ...range(
                    firstPageInResultSet,
                    firstPageInResultSet + numResultPages
                )
            );
        }
        return slots;
    }
}

// Returns the number of results of results to fetch.
// The first page of the paginated shows information on a maxiumum of five pages: [1 2 3 4 ...],
// so we need to fetch four full pages plus one more item. On subsequent pages, since we already
// have the cursor for the previous page, we only need to fetch three full pages plus one more item.
function getFetchCount(currentPageNum: number, pageSize: number) {
    const numPagesToFetch = currentPageNum === 1 ? 4 : 3;
    return pageSize * numPagesToFetch + 1;
}

// A function that will return a cursor value for a resource in the paginated set.
export type CursorExtractor<T> = (arg: T) => string;

// TODO(mreifman): Determine how to easily present the state needed for the pagination UI.
interface PaginationState<T> {
    // Current page of results being displayed in the UI.
    currentPageNum: number;
    // Start cursor for the range of results that needs to be fetched.
    fetchStartCursor: string;
    // Number of results exposed to the client for how many results need to be fetched.
    fetchCount: number;
    // Slots for rendering the pagination UI component
    slots: SlotItem[];
    // Map of page numbers to the cursor of the first item on the page. Used for fetching subsequent
    // pages of results.
    pageCursorMap: Record<number, string>;
    // Number of items on a page of results in the UI.
    pageSize: number;
    // Function for extracting the cursor value from a result in the paginated set.
    // TODO(mreifman): Is it a bad idea to put a function in this state?
    cursorExtractor: CursorExtractor<T>;
    //
    resultsPage: T[];
}

export enum PaginationActionType {
    RESET = "RESET",
    SET_CURRENT_PAGE = "SET_CURRENT_PAGE",
    SET_RESULTS = "SET_RESULTS",
}

export type ResetPayload<T> = UsePaginationArgs<T>;
export type SetCurrentPagePayload = number;
export type SetResultsPayload<T> = T[];

export interface PaginationAction<T> {
    type: PaginationActionType;
    payload?: SetCurrentPagePayload | SetResultsPayload<T> | ResetPayload<T>;
}

type PaginationReducer<PaginationState, PaginationAction> = (
    // TODO(mreifman): Consider exposing a subset of the state to hook consumers.
    // Need to figure out what state is needed for the pagination component.
    state: PaginationState,
    action: PaginationAction
) => PaginationState;

// Processes a new batch of results by storing the start cursors for each page and updating the
// slots for the pagination component.
function setResultsReducer<T>(
    state: PaginationState<T>,
    results: T[]
): PaginationState<T> {
    // TODO(mreifman): Consider storing the results to prevent re-fetching pages we already have.

    // Store the start cursors for the visible pages
    const chunks = chunk<T>(results, state.pageSize);
    chunks.forEach((c, i) => {
        // Only consider whole pages
        if (c.length === state.pageSize) {
            state.pageCursorMap[i + 1] = state.cursorExtractor(c[0]);
        }
    });

    return {
        ...state,
        slots: getSlotsForResultSet(
            results,
            state.pageSize,
            state.currentPageNum
        ),
        // TODO(mreifman): Add unit tests for resultsPage.
        resultsPage: chunks[0] || [],
    };
}

// Updates the current page and the fetch parameters needed for requesting a new batch of results.
function setCurrentPageReducer<T>(
    state: PaginationState<T>,
    selectedPageNum: number
): PaginationState<T> {
    const currentPageNum = selectedPageNum;
    const fetchCount = getFetchCount(currentPageNum, state.pageSize);
    const fetchStartCursor = state.pageCursorMap[currentPageNum];
    return {
        ...state,
        currentPageNum,
        fetchCount,
        fetchStartCursor,
    };
}

function reducer<T>(
    state: PaginationState<T>,
    action: PaginationAction<T>
): PaginationState<T> {
    const { type, payload } = action;
    switch (type) {
        case PaginationActionType.SET_CURRENT_PAGE:
            return setCurrentPageReducer(
                state,
                payload as SetCurrentPagePayload
            );
        case PaginationActionType.SET_RESULTS:
            return setResultsReducer(state, payload as SetResultsPayload<T>);
        case PaginationActionType.RESET:
            return getInitialState(payload as ResetPayload<T>);
        default:
            return state;
    }
}

interface UsePaginationState<T> {
    state: PaginationState<T>;
    dispatch: React.Dispatch<PaginationAction<T>>;
}

interface UsePaginationArgs<T> {
    startCursor: string;
    pageSize: number;
    cursorExtractor: CursorExtractor<T>;
}

function getInitialState<T>({
    startCursor,
    pageSize,
    cursorExtractor,
}: UsePaginationArgs<T>): PaginationState<T> {
    const currentPageNum = 1;
    const fetchCount = getFetchCount(currentPageNum, pageSize);

    return {
        fetchStartCursor: startCursor,
        currentPageNum,
        fetchCount,
        slots: [],
        pageCursorMap: {},
        pageSize,
        cursorExtractor,
        resultsPage: [],
    };
}

function usePagination<T>(args: UsePaginationArgs<T>): UsePaginationState<T> {
    // TODO(mreifman): If usePagination ends up doing nothing more than just calling useReducer,
    // consider having components call useReducer directly.
    const [state, dispatch] = useReducer<
        PaginationReducer<PaginationState<T>, PaginationAction<T>>
    >(reducer, getInitialState(args));
    return {
        state,
        dispatch,
    };
}

export default usePagination;
