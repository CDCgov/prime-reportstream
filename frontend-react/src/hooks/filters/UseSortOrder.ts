import { Dispatch, useReducer } from "react";

enum SortActionType {
    CHANGE_COL = "change-column",
    SWAP_ORDER = "swap-order",
    RESET = "reset",
}

type SortOrder = "ASC" | "DESC";

interface SortAction {
    type: SortActionType;
    payload?: Partial<SortSettings>;
}
interface SortSettings {
    column: string;
    order: SortOrder;
}
interface SortFilter {
    settings: SortSettings;
    update: Dispatch<SortAction>;
}

const sortSettingsReducer = (
    state: SortSettings,
    action: SortAction
): SortSettings => {
    const { type, payload } = action;
    switch (type) {
        case SortActionType.CHANGE_COL:
            return {
                column: payload?.column || state.column,
                order: state.order,
            };
        case SortActionType.SWAP_ORDER:
            return {
                column: state.column,
                order: state.order === "ASC" ? "DESC" : "ASC",
            };
        case SortActionType.RESET: // Also able to manually update settings
            return {
                column: payload?.column || "",
                order: payload?.order || "DESC",
            };
        default:
            return state;
    }
};

const useSortOrder = (): SortFilter => {
    const [settings, dispatchSettings] = useReducer(sortSettingsReducer, {
        column: "",
        order: "DESC",
    });

    return {
        settings,
        update: dispatchSettings,
    };
};

export default useSortOrder;
export { SortActionType };
export type { SortOrder, SortSettings, SortAction };
