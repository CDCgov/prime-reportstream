/*    Pagination behavior
 * 1. Load page. Start range filter is used to dictate first cursor
 * 2. On response, store cursor + nextCursor in Map<number, string> with 1-indexing to mimic page numbers
 * 3. When updateStartRange is called, conditionally update Map with new cursor at new index
 * 4. When paging back, use the Map to retrieve the previous cursor and set it
 * 5. When paging forward, use the Map to retrieve the next cursor and set it
 * 6. When calling a page, the cursor should be set to currentIndex cursor and endCursor should be set
 *    to currentIndex + 1 cursor
 */
import { useEffect, useState } from "react";

import SubmissionsResource from "../resources/SubmissionsResource";
import { FilterState } from "../pages/submissions/FilterContext";

/* TODO: Refactor to include generics so this can be used universally */
function usePaginator(
    submissions: SubmissionsResource[],
    filters: FilterState,
    updateCursor: Function
) {
    /* Cursor 1 is given the value of "" so that page 1 always shows
     */
    const [cursors, updateCursors] = useState<Map<number, string>>(
        new Map([[1, ""]])
    );
    const [currentIndex, updateCurrentIndex] = useState<number>(1);
    const [hasNext, setHasNext] = useState(false);
    const [hasPrev, setHasPrev] = useState(false);

    const pageCount = () => {
        return cursors.size;
    };

    const changeCursor = (cursorIndex: number) => {
        let cursor =
            cursors.get(cursorIndex) || cursors.get(currentIndex) || null;
        if (cursorIndex === 1 || !cursor) {
            /* Cursor 1 will always have the value of "" so that page 1 always has
             * the absolute latest results pulled. However, this will return null
             * if called cursors.get(1), so this check manually sets the cursor to
             * an empty string.
             *
             * For this, we also must reset the Map so previously tracked
             * cursors are lost and new cursors can be stored.
             */
            cursor = "";
            updateCursors(new Map([[1, cursor]]));
            updateCurrentIndex(cursorIndex);
        } else {
            if (cursor === cursors.get(cursorIndex))
                /* If it's able to retrieve your cursor, your current index
                 * updates to the cursorIndex you passed in.
                 */
                updateCurrentIndex(cursorIndex);
        }

        updateCursor(cursor);
    };

    useEffect(() => {
        const cursorExists = (c: string) => {
            return Array.from(cursors.values()).includes(c);
        };
        const addCursors = (nextCursor: string) => {
            updateCursors(cursors.set(currentIndex + 1, nextCursor));
        };
        const updateNextPrevBooleans = () => {
            setHasNext(currentIndex < cursors.size);
            setHasPrev(currentIndex > 1);
        };

        const lastTimestamp =
            submissions[filters.pageSize - 1]?.createdAt || null;
        if (lastTimestamp && !cursorExists(lastTimestamp))
            addCursors(lastTimestamp);
        updateNextPrevBooleans();

        debugger;
    }, [
        currentIndex,
        updateCurrentIndex,
        cursors,
        submissions,
        filters.pageSize,
    ]);

    /* Gives handlers for all pagination needs!
     *
     * - hasPrev/hasNext: boolean, indicating previous and next pages exist.
     * - currentIndex: number, the page number you're currently on.
     * - changeCursor: function(desiredCursorIndex), handles cursor navigation and updating.
     * - pageCount: function(), returns the current size of your cursor Map
     */
    return { hasPrev, hasNext, currentIndex, changeCursor, pageCount };
}

export default usePaginator;
