import { Dispatch, useReducer } from "react";

enum SortSettingsActionType {
    CHANGE_COL = "change-column",
    SWAP_ORDER = "swap-order",
    RESET = "reset",
}

type SortOrder = "ASC" | "DESC";

interface SortSettingsAction {
    type: SortSettingsActionType;
    payload?: Partial<SortSettings>;
}
interface SortSettings {
    column: string;
    order: SortOrder;
}
interface SortFilter {
    settings: SortSettings;
    update: Dispatch<SortSettingsAction>;
}

const sortSettingsReducer = (
    state: SortSettings,
    action: SortSettingsAction
): SortSettings => {
    const { type, payload } = action;
    switch (type) {
        case SortSettingsActionType.CHANGE_COL:
            return {
                column: payload?.column || state.column,
                order: state.order,
            };
        case SortSettingsActionType.SWAP_ORDER:
            return {
                column: state.column,
                order: state.order === "ASC" ? "DESC" : "ASC",
            };
        case SortSettingsActionType.RESET: // Also able to manually update settings
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
export { SortSettingsActionType };
export type { SortOrder, SortSettings, SortSettingsAction };
