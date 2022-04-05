import React, { useState } from "react";

import { SortOrder } from "../components/Table/Table";
import { PageSize } from "../contexts/FilterContext";


export type StateUpdate<T> = React.Dispatch<React.SetStateAction<T>>;

export interface FilterState {
    startRange: string;
    endRange: string;
    sortOrder: SortOrder;
    pageSize: PageSize;
}

export interface FilterController {
    setStartRange: StateUpdate<string>;
    setEndRange: StateUpdate<string>;
    swapSortOrder: () => void;
    setPageSize: StateUpdate<PageSize>;
}

export interface IFilterManager {
    filters: FilterState;
    update: FilterController;
}

const useFilterManager = (init?: Partial<FilterState>): IFilterManager => {
    const [startRange, setStartRange] = useState(init?.startRange || "");
    const [endRange, setEndRange] = useState(init?.endRange || "");
    const [sortOrder, setSortOrder] = useState<SortOrder>(
        init?.sortOrder || "DESC"
    );
    const [pageSize, setPageSize] = useState<PageSize>(init?.pageSize || 10);

    const swapSortOrder = () => {
        switch (sortOrder) {
            case "DESC":
                setSortOrder("ASC");
                break;
            case "ASC":
                setSortOrder("DESC");
                break;
        }
    };

    return {
        filters: {
            startRange,
            endRange,
            sortOrder,
            pageSize,
        },
        update: {
            setStartRange,
            setEndRange,
            swapSortOrder,
            setPageSize,
        },
    };
};

export default useFilterManager;
