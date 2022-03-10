import React, { createContext, PropsWithChildren, useState } from "react";

type SortOrder = "ASC" | "DESC";
type PageSize = 10 | 25 | 50 | 100;

export interface FilterState {
    startRange: string;
    endRange: string;
    sortOrder: SortOrder;
    pageSize: PageSize;
}

interface ISubmissionFilterContext {
    filters: FilterState;
    updateStartRange?: (val: string) => void;
    updateEndRange?: (val: string) => void;
    updateSortOrder?: (val: SortOrder) => void;
    updatePageSize?: (val: PageSize) => void;
}

/* This is a definition of the context shape, NOT the payload delivered */
export const SubmissionFilterContext = createContext<ISubmissionFilterContext>({
    filters: {
        startRange: "",
        endRange: "",
        sortOrder: "DESC",
        pageSize: 10,
    },
});

/*
 * This component handles a pseudo-global state for the Submission
 * components; primarily linking SubmissionTable and
 * SubmissionFilters. This is much friendlier than callback functions
 * and piping props!
 */
const FilterContext: React.FC<any> = (props: PropsWithChildren<any>) => {
    const [startRange, setStartRange] = useState<string>("");
    const [endRange, setEndRange] = useState<string>("");
    const [sortOrder, setSortOrder] = useState<SortOrder>("DESC");
    const [pageSize, setPageSize] = useState<PageSize>(10);

    const updateStartRange = (val: string) => setStartRange(val);
    const updateEndRange = (val: string) => setEndRange(val);
    const updateSortOrder = (val: SortOrder) => setSortOrder(val);
    const updatePageSize = (val: PageSize) => setPageSize(val);

    /* This is the payload we deliver through our context provider */
    const contextPayload: ISubmissionFilterContext = {
        filters: {
            startRange: startRange,
            endRange: endRange,
            sortOrder: sortOrder,
            pageSize: pageSize,
        },
        updateStartRange: updateStartRange,
        updateEndRange: updateEndRange,
        updateSortOrder: updateSortOrder,
        updatePageSize: updatePageSize,
    };

    return (
        <SubmissionFilterContext.Provider value={contextPayload}>
            {props.children}
        </SubmissionFilterContext.Provider>
    );
};

export default FilterContext;
