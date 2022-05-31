import { Dispatch, useReducer } from "react";

enum SortSettingsActionType {
    CHANGE_COL = "change-column",
    SWAP_ORDER = "swap-order",
    APPLY_LOCAL_SORT = "apply-local-sort",
    SWAP_LOCAL_ORDER = "swap-local-order",
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
    localOrder: SortOrder;
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
        case SortSettingsActionType.SWAP_LOCAL_ORDER:
            return {
                ...state,
                localOrder: state.localOrder === "ASC" ? "DESC" : "ASC",
            };
        case SortSettingsActionType.RESET: // Also able to manually update settings
            return {
                column: payload?.column || "",
                order: payload?.order || "DESC",
                locally: payload?.locally || false,
                localOrder: payload?.localOrder || "DESC",
            };
        default:
            return state;
    }
};

const useSortOrder = (options?: Partial<SortSettings>): SortFilter => {
    const [settings, dispatchSettings] = useReducer(sortSettingsReducer, {
        column: options?.column || "",
        order: options?.order || "DESC",
        locally: options?.locally || false,
        localOrder: options?.localOrder || "DESC",
    });

    return {
        settings,
        update: dispatchSettings,
    };
};

export default useSortOrder;
export { SortSettingsActionType };
export type { SortOrder, SortSettings, SortSettingsAction };
