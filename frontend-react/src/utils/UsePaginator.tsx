/* Pagination behavior:
 * 1. Initial cursor set to "", which loads all results.
 * 2. On response, stores next cursor in Map<number, string>
 * 3. Calling changeCursor(index) from the consumer will update the cursor using
 *    the provided updateCursor function.
 *
 * Notes:
 * - When paging back and forth, the map will not add any new cursors
 *   it detects are already present.
 * - Going back to Page 1 wipes the map and starts over as new elements might be
 *   present in the response, making old cursors irrelevant.
 */
import { useCallback, useEffect, useState } from "react";

import SubmissionsResource from "../resources/SubmissionsResource";
import { FilterState } from "../pages/submissions/FilterContext";

interface PaginationController {
    hasPrev: boolean;
    hasNext: boolean;
    currentIndex: number;
    changeCursor?: (cursorIndex: number) => void;
    pageCount?: () => number;
}

/* TODO: Refactor to include generics so this can be used universally */
/* Pagination hook inputs
 * @param responseArray: an array of items sent back from the API
 * @param filters: your FilterState including a startRange and endRange property
 * @param updateCursor: a function that will update your startRange when called
 */
function usePaginator(
    responseArray: SubmissionsResource[],
    filterState: FilterState,
    updateCursor: Function
) {
    const today = new Date().toISOString();
    const [cursors, updateCursors] = useState<Map<number, string>>(
        new Map([[1, today]])
    );
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

    const resetCursors = (c: string) => {
        const cursor = c;
        updateCursors(new Map([[1, cursor]]));
        updateCurrentIndex(1);
        updateCursor(cursor);
    };

    const changeCursor = (
        cursorIndex: number,
        firstCursorOverride?: string
    ) => {
        let cursor =
            cursors.get(cursorIndex) || cursors.get(currentIndex) || null;
        if (cursorIndex === 1 || !cursor) {
            /* Page 1's cursor is defaulted to an empty string to ensure
             * it always loads the most recent results.
             */
            resetCursors(firstCursorOverride || today);
        } else {
            if (cursor === cursors.get(cursorIndex))
                /* If it's able to retrieve your cursor, your current index
                 * updates to the cursorIndex you passed in.
                 */
                updateCurrentIndex(cursorIndex);
        }

        updateCursor(cursor);
    };

    /* Adds new cursors to end of map */
    useEffect(() => {
        const lastTimestamp =
            responseArray[filterState.pageSize - 1]?.createdAt || null;
        if (lastTimestamp && !cursorExists(lastTimestamp))
            updateCursors(cursors.set(currentIndex + 1, lastTimestamp));
        setHasNext(currentIndex < cursors.size);
        setHasPrev(currentIndex > 1);
    }, [
        cursorExists,
        currentIndex,
        updateCurrentIndex,
        cursors,
        responseArray,
        filterState.pageSize,
    ]);

    /* Gives handlers for all pagination needs!
     *
     * - hasPrev/hasNext: boolean, indicating previous and next pages exist.
     * ---> Used for conditionally showing Next/Prev buttons
     * - currentIndex: number, the page number you're currently on.
     * - changeCursor: function(desiredCursorIndex), handles cursor navigation and updating.
     * - pageCount: function(), returns the current size of your cursor Map
     */

    const paginator: PaginationController = {
        hasPrev: hasPrev,
        hasNext: hasNext,
        currentIndex: currentIndex,
        changeCursor: changeCursor,
        pageCount: pageCount,
    };

    return paginator;
}

export default usePaginator;
