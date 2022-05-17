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
    action: RangeSettingsAction
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

const FALLBACK_FROM = new Date("2000-01-01").toISOString();
const FALLBACK_TO = new Date("3000-01-01").toISOString();

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
export { RangeSettingsActionType, RangeField, FALLBACK_FROM, FALLBACK_TO };
export type { RangeSettings, RangeSettingsAction };
