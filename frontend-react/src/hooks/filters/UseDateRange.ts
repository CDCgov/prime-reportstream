import { Dispatch, useReducer } from "react";

enum DateRangeActionType {
    UPDATE_START = "update-start",
    UPDATE_END = "update-end",
    RESET = "reset",
}

interface DateRange {
    start: string;
    end: string;
}
interface DateRangeAction {
    type: DateRangeActionType;
    payload?: Partial<DateRange>;
}

interface RangeFilter {
    settings: DateRange;
    update: Dispatch<DateRangeAction>;
}

const rangeReducer = (state: DateRange, action: DateRangeAction): DateRange => {
    const { type, payload } = action;
    switch (type) {
        case DateRangeActionType.UPDATE_START:
            return {
                ...state,
                start: payload?.start || state.start,
            };
        case DateRangeActionType.UPDATE_END:
            return {
                ...state,
                end: payload?.end || state.end,
            };
        case DateRangeActionType.RESET: // Can use this to manually set for edge cases
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
export { DateRangeActionType };
export type { DateRange };
