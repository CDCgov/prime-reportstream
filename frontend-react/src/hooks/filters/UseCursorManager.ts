import { useCallback, useMemo, useState } from "react";

type CursorMap = Map<number, string>;

interface ICursorValues {
    cursor: string;
    cursors: CursorMap;
    currentIndex: number;
    hasPrev: boolean;
    hasNext: boolean;
}

interface ICursorController {
    addNextCursor: (val: string) => void;
    goTo: (i: number) => void;
    reset: (c?: string) => void;
}

interface CursorManager {
    values: ICursorValues;
    controller: ICursorController;
}

/* CursorManager handles logic to maintain an accurate map of cursors by page.
 * Each time you append a cursor with `addNextCursor`, the manager will handle,
 * duplicates and set your hasNext and hasPrev values for you. */
const useCursorManager = (firstCursor?: string) => {
    const [cursors, setCursors] = useState<CursorMap>(
        new Map<number, string>([[0, firstCursor || ""]])
    );
    const [currentIndex, setCurrentIndex] = useState<number>(0);
    const cursor = useMemo(() => {
        return cursors.get(currentIndex) || "";
    }, [cursors, currentIndex]);
    const [hasPrev, setHasPrev] = useState<boolean>(false);
    const [hasNext, setHasNext] = useState<boolean>(false);

    /* Handles verifying cursor duplication */
    const cursorExists = useCallback(
        (c: string) => {
            return Array.from(cursors.values()).includes(c);
        },
        [cursors]
    );

    /* Checks for next and previous values in map from currentIndex OR from
     * passed-in index. */
    const checkNextPrev = useCallback(
        (i?: number) => {
            setHasNext(cursors.get((i || currentIndex) + 1) !== undefined);
            setHasPrev(cursors.get((i || currentIndex) - 1) !== undefined);
        },
        [currentIndex, cursors]
    );

    /* Private method for handling cursor addition since we may not always
     * want to just append a cursor. Checks for duplicates and won't add
     * duplicate cursors. */
    const addCursorNoDuplicates = (index: number, val: string) => {
        if (!cursorExists(val)) {
            setCursors(cursors.set(index, val));
        }
    };

    /* Handles simple appending of new cursors to the end of the Map */
    const addNextCursor = (val: string) => {
        addCursorNoDuplicates(cursors.size, val);
        checkNextPrev();
    };

    /* Handles navigating the cursor map */
    const goTo = (i: number) => {
        if (i >= cursors.size) {
            setCurrentIndex(currentIndex);
        } else {
            setCurrentIndex(i);
            checkNextPrev(i);
        }
    };

    /* Can reset the cursor map and current index  */
    const resetCursors = (newFirst?: string) => {
        setCursors(
            new Map<number, string>([[0, newFirst || firstCursor || ""]])
        );
        setCurrentIndex(0);
    };

    /* Returning values and controllers as objects to keep them clean */
    const values: ICursorValues = useMemo(() => {
        return {
            cursor: cursor,
            cursors: cursors,
            currentIndex: currentIndex,
            hasPrev: hasPrev,
            hasNext: hasNext,
        };
    }, [cursor, cursors, currentIndex, hasPrev, hasNext]);

    const controller: ICursorController = {
        addNextCursor: addNextCursor,
        goTo: goTo,
        reset: resetCursors,
    };

    return { values, controller };
};

export default useCursorManager;
export type { ICursorValues, ICursorController, CursorManager };
