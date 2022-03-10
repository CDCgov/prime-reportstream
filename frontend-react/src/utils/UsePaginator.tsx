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
import { useEffect, useState } from "react";

import SubmissionsResource from "../resources/SubmissionsResource";
import {PageSize} from "../pages/submissions/FilterContext";

/* TODO: Refactor to include generics so this can be used universally */
/* Pagination hook inputs
 * @param responseArray: an array of items sent back from the API
 * @param filters: your FilterState
 */
function usePaginator(
    responseArray: SubmissionsResource[],
    pageSize: PageSize,
    updateCursor: Function
) {
    /* Cursor 1 is given the value of "" so that page 1 always shows
     * the latest values.
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
            responseArray[pageSize - 1]?.createdAt || null;
        if (lastTimestamp && !cursorExists(lastTimestamp))
            addCursors(lastTimestamp);
        updateNextPrevBooleans();
    }, [
        currentIndex,
        updateCurrentIndex,
        cursors,
        responseArray,
        pageSize,
    ]);

    /* Gives handlers for all pagination needs!
     *
     * - hasPrev/hasNext: boolean, indicating previous and next pages exist.
     * ---> Used for conditionally showing Next/Prev buttons
     * - currentIndex: number, the page number you're currently on.
     * - changeCursor: function(desiredCursorIndex), handles cursor navigation and updating.
     * - pageCount: function(), returns the current size of your cursor Map
     */
    return { hasPrev, hasNext, currentIndex, changeCursor, pageCount };
}

export default usePaginator;
