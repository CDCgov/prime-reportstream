import { Dispatch, useReducer } from "react";

enum SortSettingsActionType {
    CHANGE_COL = "change-column",
    SWAP_ORDER = "swap-order",
    APPLY_LOCAL_SORT = "apply-local-sort",
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
    locally: boolean;
}
interface SortFilter {
    settings: SortSettings;
    update: Dispatch<SortSettingsAction>;
}

export const sortSettingsReducer = (
    state: SortSettings,
    action: SortSettingsAction
): SortSettings => {
    const { type, payload } = action;
    switch (type) {
        case SortSettingsActionType.CHANGE_COL:
            return {
                ...state,
                column: payload?.column || state.column,
            };
        case SortSettingsActionType.SWAP_ORDER:
            return {
                ...state,
                order: state.order === "ASC" ? "DESC" : "ASC",
            };
        case SortSettingsActionType.APPLY_LOCAL_SORT:
            return {
                ...state,
                locally: payload?.locally || false,
            };
        case SortSettingsActionType.RESET: // Also able to manually update settings
            return {
                column: payload?.column || "",
                order: payload?.order || "DESC",
                locally: payload?.locally || false,
            };
        default:
            return state;
    }
};

const useSortOrder = (): SortFilter => {
    const [settings, dispatchSettings] = useReducer(sortSettingsReducer, {
        column: "",
        order: "DESC",
        locally: false,
    });

    return {
        settings,
        update: dispatchSettings,
    };
};

export default useSortOrder;
export { SortSettingsActionType };
export type { SortOrder, SortSettings, SortSettingsAction };
