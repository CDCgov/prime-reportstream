import { Dispatch, useReducer } from "react";

enum RangeField {
    FROM = "from",
    TO = "to",
}
enum RangeSettingsActionType {
    UPDATE_FROM = "update-from",
    UPDATE_TO = "update-to",
    RESET = "reset",
}

interface RangeSettings {
    from: string;
    to: string;
}
interface RangeSettingsAction {
    type: RangeSettingsActionType;
    payload?: Partial<RangeSettings>;
}

interface RangeFilter {
    settings: RangeSettings;
    update: Dispatch<RangeSettingsAction>;
}

const rangeReducer = (
    state: RangeSettings,
    action: RangeSettingsAction,
): RangeSettings => {
    const { type, payload } = action;
    switch (type) {
        case RangeSettingsActionType.UPDATE_FROM:
            return {
                ...state,
                from: payload?.from || state.from,
            };
        case RangeSettingsActionType.UPDATE_TO:
            return {
                ...state,
                to: payload?.to || state.to,
            };
        case RangeSettingsActionType.RESET: // Can use this to manually set for edge cases
            return {
                from: payload?.from || FALLBACK_FROM,
                to: payload?.to || FALLBACK_TO,
            };
        default:
            return state;
    }
};

export function getEndOfDay(date: Date) {
    // return a new Date instance so we don't mutate old one
    const newDate = new Date(date);
    newDate.setUTCHours(23, 59, 59, 999);
    return newDate;
}

const FALLBACK_FROM_STRING = "01/01/2000";
const FALLBACK_FROM = new Date(FALLBACK_FROM_STRING).toISOString();
const FALLBACK_TO_STRING = "01/01/3000";
const FALLBACK_TO = new Date(FALLBACK_TO_STRING).toISOString();

const useDateRange = (): RangeFilter => {
    const [settings, dispatchRange] = useReducer(rangeReducer, {
        from: FALLBACK_FROM,
        to: FALLBACK_TO,
    });

    return {
        settings,
        update: dispatchRange,
    };
};

export default useDateRange;
export {
    RangeSettingsActionType,
    RangeField,
    FALLBACK_FROM,
    FALLBACK_TO,
    FALLBACK_FROM_STRING,
    FALLBACK_TO_STRING,
};
export type { RangeSettings, RangeSettingsAction };
