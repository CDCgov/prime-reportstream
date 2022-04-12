import { useCallback, useState } from "react";

import { SortOrder } from "./UseSortOrder";

type DateRangeSetter = ({ date1, date2, sort }: SetRangeParams) => void;
interface DateRange {
    startRange: Date;
    endRange: Date;
}
interface SetRangeParams {
    date1: string;
    date2?: string;
    sort?: SortOrder;
}
interface DateRange {
    startRange: Date;
    endRange: Date;
    setRange: DateRangeSetter;
    resetRange: () => void;
}

const FALLBACK_START = new Date("2998-01-01");
const FALLBACK_END = new Date("2000-01-01");

const useDateRange = (init?: Partial<DateRange>): DateRange => {
    const [startRange, setStartRange] = useState(
        init?.startRange || FALLBACK_START
    );
    const [endRange, setEndRange] = useState(init?.endRange || FALLBACK_END);

    const set = useCallback(({ date1, date2, sort }: SetRangeParams) => {
        if (!date2) {
            /* If one date is given, this is a cursor update */
            const date1AsDate = new Date(date1);
            if (sort === "DESC") {
                /* Descending cursor updates must incrementally narrow
                 * the range from Start -> End */
                setStartRange(date1AsDate);
            } else {
                /* Ascending cursor updates must incrementally narrow
                 * the range from End -> Start */
                setEndRange(date1AsDate);
            }
        } else {
            /* If both dates are given, this is a range filter set */
            setStartRange(new Date(date1));
            setEndRange(new Date(date2));
        }
    }, []);

    const reset = useCallback(() => {
        setStartRange(init?.startRange || FALLBACK_START);
        setEndRange(init?.endRange || FALLBACK_END);
    }, [init]);

    return {
        startRange,
        endRange,
        setRange: set,
        resetRange: reset,
    };
};

export default useDateRange;
export type { DateRange, SetRangeParams, DateRangeSetter };
