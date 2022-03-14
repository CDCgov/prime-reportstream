/* Pagination behavior:
 * First, a pagination button will call changeCursor(index). This will
 * trigger a re-call by updating the cursor in FilterContext with the
 * cursor found at Map.get(index). Next, the returned submissions will
 * trigger an effect that updates the Map with the next cursor in the
 * bunch (i.e. our +1 item from the API call).
 *
 * When navigating back to the first page, the cursor map will reset
 * cursors 1 and 2 with the current value of startRange from FilterContext.
 * If this is a user-defined value, it means they have a startRange filter
 * applied, and nothing more recent than that date will show. If no filter
 * is set, it defaults to "", meaning the first n-many items in the DB will
 * return (based on page-size).
 */
import { useCallback, useEffect, useState } from "react";

import SubmissionsResource from "../resources/SubmissionsResource";
import { FilterState } from "../pages/submissions/FilterContext";

export interface PaginationController {
    cursors: Map<number, string>;
    hasPrev: boolean;
    hasNext: boolean;
    currentIndex: number;
    changeCursor?: (cursorIndex: number) => void;
    pageCount?: () => number;
    resetCursors?: () => void;
}

/* TODO: Refactor to include generics so this can be used universally */
/* Pagination hook inputs
 * @param responseArray: an array of items sent back from the API
 * @param filters: your FilterState including a startRange and endRange property
 * @param updateCursor: a function that will update your startRange when called
 */
function usePaginator(
    responseArray: SubmissionsResource[],
    filterState: FilterState
) {
    const [cursors, updateCursors] = useState<Map<number, string>>(new Map());
    const [currentIndex, updateCurrentIndex] = useState<number>(1);
    const [hasNext, setHasNext] = useState(false);
    const [hasPrev, setHasPrev] = useState(false);

    const pageCount = () => {
        return cursors.size;
    };

    const cursorExists = useCallback(
        (c: string) => {
            return Array.from(cursors.values()).includes(c);
        },
        [cursors]
    );

    const resetCursors = useCallback(() => {
        return updateCursors(new Map<number, string>());
    }, []);

    const changeCursor = useCallback((index: number) => {
        updateCurrentIndex(index);
    }, []);

    /* Handles adding cursors in the Map when responseArray changes */
    useEffect(() => {
        const nextCursor =
            responseArray[filterState.pageSize - 1]?.createdAt || null;
        if (currentIndex === 1) {
            // add first item with value of filterState.startRange
            updateCursors(cursors.set(1, filterState.startRange));
            if (nextCursor) updateCursors(cursors.set(2, nextCursor));
        }
        if (nextCursor && !cursorExists(nextCursor)) {
            // add next item with value of cursor
            updateCursors(cursors.set(cursors.size + 1, nextCursor));
        }
        setHasNext(currentIndex < cursors.size);
        setHasPrev(currentIndex > 1);
    }, [
        currentIndex,
        cursorExists,
        cursors,
        filterState.pageSize,
        filterState.startRange,
        responseArray,
    ]);

    /* Gives handlers for all pagination needs!
     *
     * - hasPrev/hasNext: boolean, indicating previous and next pages exist.
     * - currentIndex: where you currently are in the page map.
     * - changeCursor: function(desiredCursorIndex), handles cursor navigation and updating.
     * - pageCount: function(), returns the current size of your cursor Map.
     */

    const paginator: PaginationController = {
        cursors: cursors,
        hasPrev: hasPrev,
        hasNext: hasNext,
        currentIndex: currentIndex,
        changeCursor: changeCursor,
        pageCount: pageCount,
        resetCursors: resetCursors,
    };

    return paginator;
}

export default usePaginator;
