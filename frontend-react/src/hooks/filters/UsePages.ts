import { Dispatch, useReducer } from "react";

enum PageSettingsActionType {
    INC_PAGE = "increment",
    DEC_PAGE = "decrement",
    SET_SIZE = "set-size",
    RESET = "reset",
}

type PageSize = 10 | 25 | 50 | 100;

interface PageSettings {
    size: PageSize;
    currentPage: number;
}
interface PageFilter {
    settings: PageSettings;
    update: Dispatch<PageSettingsAction>;
}

interface PageSettingsAction {
    type: PageSettingsActionType;
    payload?: Partial<PageSettings>;
}

const pageNumReducer = (
    state: PageSettings,
    action: PageSettingsAction
): PageSettings => {
    const { type, payload } = action;
    switch (type) {
        case PageSettingsActionType.DEC_PAGE:
            return {
                ...state,
                currentPage: state.currentPage - 1,
            };
        case PageSettingsActionType.INC_PAGE:
            return {
                ...state,
                currentPage: state.currentPage + 1,
            };
        case PageSettingsActionType.SET_SIZE:
            return {
                ...state,
                size: payload?.size || state.size,
            };
        case PageSettingsActionType.RESET:
            return {
                ...state,
                currentPage: 1,
            };
        default:
            return state;
    }
};

const usePages = (defaults?: Partial<PageSettings>): PageFilter => {
    const [settings, dispatchSettings] = useReducer(pageNumReducer, {
        size: defaults?.size || 10,
        /* TODO: this can be removed if new pagination tracks page number */
        currentPage: defaults?.currentPage || 1,
    });

    return {
        settings,
        update: dispatchSettings,
    };
};

export default usePages;
export { PageSettingsActionType };
export type { PageSize, PageSettings, PageSettingsAction };
