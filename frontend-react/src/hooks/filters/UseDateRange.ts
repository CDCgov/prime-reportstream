import { Dispatch, useReducer } from "react";

enum RangeField {
    START = "start",
    END = "end",
}
enum RangeSettingsActionType {
    UPDATE_START = "update-start",
    UPDATE_END = "update-end",
    RESET = "reset",
}

interface RangeSettings {
    start: string;
    end: string;
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
        case RangeSettingsActionType.UPDATE_START:
            return {
                ...state,
                start: payload?.start || state.start,
            };
        case RangeSettingsActionType.UPDATE_END:
            return {
                ...state,
                end: payload?.end || state.end,
            };
        case RangeSettingsActionType.RESET: // Can use this to manually set for edge cases
            return {
                start: payload?.start || FALLBACK_START,
                end: payload?.end || FALLBACK_END,
            };
        default:
            return state;
    }
};

const FALLBACK_START = new Date("3000-01-01").toISOString();
const FALLBACK_END = new Date("2000-01-01").toISOString();

const useDateRange = (): RangeFilter => {
    const [settings, dispatchRange] = useReducer(rangeReducer, {
        start: FALLBACK_START,
        end: FALLBACK_END,
    });

    return {
        settings,
        update: dispatchRange,
    };
};

export default useDateRange;
export { RangeSettingsActionType, RangeField };
export type { RangeSettings, RangeSettingsAction };
