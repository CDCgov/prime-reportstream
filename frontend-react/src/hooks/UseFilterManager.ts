import React, { useState } from "react";

type SortOrder = "ASC" | "DESC";
export type PageSize = 10 | 25 | 50 | 100;
export type StateUpdate<T> = React.Dispatch<React.SetStateAction<T>>;

interface SortSettings {
    column: string;
    order: SortOrder;
}

export interface FilterState {
    startRange: string;
    endRange: string;
    sort: SortSettings;
    pageSize: PageSize;
}

export interface FilterController {
    setStartRange: StateUpdate<string>;
    setEndRange: StateUpdate<string>;
    setSortSettings: (col: string) => void;
    setPageSize: StateUpdate<PageSize>;
    clearAll: () => void;
}

export interface IFilterManager {
    filters: FilterState;
    update: FilterController;
}

const useFilterManager = (init?: Partial<FilterState>): IFilterManager => {
    const [startRange, setStartRange] = useState(init?.startRange || "");
    const [endRange, setEndRange] = useState(init?.endRange || "");
    const [sort, setSort] = useState<SortSettings>(
        init?.sort || { column: "", order: "DESC" }
    );
    const [pageSize, setPageSize] = useState<PageSize>(init?.pageSize || 10);

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

    const clearAll = () => {
        setStartRange("");
        setEndRange("");
        setSort({ column: "", order: "DESC" });
        setPageSize(10);
    };

    return {
        filters: {
            startRange,
            endRange,
            sort,
            pageSize,
        },
        update: {
            setStartRange,
            setEndRange,
            setSortSettings,
            setPageSize,
            clearAll,
        },
    };
};

export default useFilterManager;
