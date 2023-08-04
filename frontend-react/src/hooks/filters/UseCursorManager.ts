import React, { useMemo, useReducer } from "react";

interface Cursors {
    current: string;
    next: string;
    history: string[];
}
enum CursorActionType {
    PAGE_UP = "page-up",
    PAGE_DOWN = "page-down",
    RESET = "reset",
    ADD_NEXT = "add-next",
}
interface CursorAction {
    type: CursorActionType;
    payload?: string;
}

type CursorReducer<Cursors, CursorAction> = (
    cursors: Cursors,
    action: CursorAction,
) => Cursors;

/** @deprecated CursorManager is replaced with PaginationProps*/
interface CursorManager {
    cursors: Cursors;
    hasPrev: boolean;
    hasNext: boolean;
    update: React.Dispatch<CursorAction>;
}

const cursorReducer = (state: Cursors, action: CursorAction): Cursors => {
    const { type, payload } = action;
    switch (type) {
        case CursorActionType.PAGE_UP:
            /* Pages up by setting new current with state.next,
             * sets next to an empty string since ADD_NEXT handles
             * that, and adds state.current to the history. */
            return {
                current: state.next,
                next: "",
                history: [...state.history, state.current],
            } as Cursors;
        case CursorActionType.PAGE_DOWN:
            /* Pages down by popping the last history item as current,
             * sets next to state.current, and spreads the remaining
             * history */
            return {
                current: state.history.pop(),
                next: state.current,
                history: [...state.history],
            } as Cursors;
        case CursorActionType.RESET:
            /* Resets all values */
            return {
                current: payload || "",
                next: "",
                history: [],
            } as Cursors;
        case CursorActionType.ADD_NEXT:
            /* Adds next cursor value */
            return {
                ...state,
                next: payload || "",
            };
        default:
            return state;
    }
};

/** @deprecated Replace with usePagination for numbered pagination */
const useCursorManager = (firstCursor?: string) => {
    const [cursors, cursorDispatch] = useReducer<
        CursorReducer<Cursors, CursorAction>
    >(cursorReducer, {
        current: firstCursor || "",
        next: "",
        history: [],
    });
    const hasPrev = useMemo(() => {
        return cursors.history.length > 0;
    }, [cursors]);
    const hasNext = useMemo(() => {
        return cursors.next !== "";
    }, [cursors]);

    return {
        cursors,
        hasNext,
        hasPrev,
        update: cursorDispatch,
    };
};

export default useCursorManager;
export { CursorActionType };
export type { CursorManager };
