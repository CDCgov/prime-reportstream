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
    action: CursorAction
) => Cursors;

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
            return {
                current: state.next,
                next: "",
                history: [...state.history, state.current],
            } as Cursors;
        case CursorActionType.PAGE_DOWN:
            return {
                current: state.history.pop(),
                next: state.current,
                history: [...state.history],
            } as Cursors;
        case CursorActionType.RESET:
            return {
                current: payload || "",
                next: "",
                history: [],
            } as Cursors;
        case CursorActionType.ADD_NEXT:
            return {
                ...state,
                next: payload || "",
            };
        default:
            return state;
    }
};

/* CursorManager handles logic to maintain an accurate map of cursors by page.
 * Each time you append a cursor with `addNextCursor`, the manager will handle,
 * duplicates and set your hasNext and hasPrev values for you. */
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
