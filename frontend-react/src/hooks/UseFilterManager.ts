import React, { useMemo, useState } from "react";

export type SortOrder = "ASC" | "DESC";
export type PageSize = 10 | 25 | 50 | 100;
export type StateUpdate<T> = React.Dispatch<React.SetStateAction<T>>;

interface DateFilterRange {
    startRange: string;
    endRange: string;
}

interface SortSettings {
    column: string;
    order: SortOrder;
}

export interface FilterState {
    dateRange: DateFilterRange;
    sort: SortSettings;
    pageSize: PageSize;
}

export interface FilterController {
    setRange: (date1: Date, date2?: Date) => void;
    setSortSettings: (col: string) => void;
    setPageSize: StateUpdate<PageSize>;
    clearAll: () => void;
}

export interface IFilterManager {
    filters: FilterState;
    update: FilterController;
}

export const FALLBACK_DATE1 = new Date().toISOString();
export const FALLBACK_DATE2 = "2020-01-01T00:00:00.000Z";

export const checkDate = (d: Date | string) =>
    typeof d === "string" ? new Date(d) : d;
export const olderOfDates = (date1: Date | string, date2: Date | string) => {
    if (checkDate(date1) < checkDate(date2)) {
        return checkDate(date1);
    } else {
        return checkDate(date2);
    }
};
export const newerOfDates = (date1: Date | string, date2: Date | string) => {
    if (checkDate(date1) > checkDate(date2)) {
        return checkDate(date1);
    } else {
        return checkDate(date2);
    }
};

const useFilterManager = (init?: Partial<FilterState>): IFilterManager => {
    const [date1, setDate1] = useState(
        init?.dateRange?.startRange || FALLBACK_DATE1
    );
    const [date2, setDate2] = useState(
        init?.dateRange?.endRange || FALLBACK_DATE2
    );
    const [sort, setSort] = useState<SortSettings>(
        init?.sort || { column: "", order: "DESC" }
    );
    const [pageSize, setPageSize] = useState<PageSize>(init?.pageSize || 10);

    /* Ensures startRange is and endRange match sort order configuration needs */
    const dateRange: DateFilterRange = useMemo(() => {
        if (sort.order === "ASC") {
            return {
                startRange: olderOfDates(date1, date2).toISOString(),
                endRange: newerOfDates(date1, date2).toISOString(),
            };
        } else {
            return {
                startRange: newerOfDates(date1, date2).toISOString(),
                endRange: olderOfDates(date1, date2).toISOString(),
            };
        }
    }, [sort.order, date1, date2]);

    /* TODO: Refactor this? Could probably  be a bit simpler */
    const setSortSettings = (col: string, order?: SortOrder) => {
        if (sort.column === col) {
            /* If the user isn't changing sort column, they're swapping
             * sort order. */
            switch (sort.order) {
                case "DESC":
                    setSort({ column: col, order: order || "ASC" });
                    break;
                case "ASC":
                    setSort({ column: col, order: order || "DESC" });
                    break;
            }
        } else {
            /* Else, the user has selected a new column, and the current
             * sort order will apply to the new column */
            setSort({ column: col, order: order || sort.order });
        }
    };

    const setRange = (date1: Date, date2?: Date) => {
        setDate1(date1.toISOString());
        if (date2) {
            setDate2(date2?.toISOString());
        }
    };

    const clearAll = () => {
        setDate1(init?.dateRange?.startRange || FALLBACK_DATE1);
        setDate2(init?.dateRange?.endRange || FALLBACK_DATE2);
        setSort(init?.sort || { column: "", order: "DESC" });
        setPageSize(init?.pageSize || 10);
    };

    return {
        filters: {
            dateRange,
            sort,
            pageSize,
        },
        update: {
            setRange,
            setSortSettings,
            setPageSize,
            clearAll,
        },
    };
};

export default useFilterManager;
