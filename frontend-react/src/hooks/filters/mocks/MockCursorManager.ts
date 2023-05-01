import { CursorManager } from "../UseCursorManager";

export const mockCursorManager: CursorManager = {
    cursors: {
        current: "",
        next: "",
        history: [],
    },
    hasPrev: false,
    hasNext: false,
    // eslint-disable-next-line no-console
    update: () => console.log(""),
};
