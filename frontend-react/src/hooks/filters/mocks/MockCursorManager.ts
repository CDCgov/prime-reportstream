import { CursorManager } from "../UseCursorManager";

export const mockCursorManager: CursorManager = {
    cursors: {
        current: "",
        next: "",
        history: [],
    },
    hasPrev: false,
    hasNext: false,
    update: () => console.log(""),
};
