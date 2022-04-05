import React, {
    createContext,
    PropsWithChildren,
    useEffect,
    useState,
} from "react";
import { useResource } from "rest-hooks";

import usePaginator, { PaginationController } from "../hooks/UsePaginator";
import SubmissionsResource from "../resources/SubmissionsResource";

import { getStoredOrg } from "./SessionStorageTools";

/* Convenient type aliases */
type SortOrder = "ASC" | "DESC";
type PageSize = 10 | 25 | 50 | 100;
type FilterTypes = string | SortOrder | PageSize;

type StateUpdater = (filter: FilterName, val?: FilterTypes) => void;
type StateClearer = () => void;

export enum FilterName {
    START_RANGE = "start-range",
    END_RANGE = "end-range",
    CURSOR = "cursor",
    SORT_ORDER = "sort-order",
    PAGE_SIZE = "page-size",
}

/* Object containing state from a FilterContext */
export interface FilterState {
    startRange: string;
    endRange: string;
    cursor: string;
    sortOrder: SortOrder;
    pageSize: PageSize;
}

/* Shape of the object provided to Submissions pages */
interface ISubmissionFilterContext {
    filters: FilterState;
    contents: SubmissionsResource[];
    updateFilter: StateUpdater;
    clear: StateClearer;
    paginator?: PaginationController;
}

/* This is a definition of the context shape, NOT the payload delivered */
export const SubmissionFilterContext = createContext<ISubmissionFilterContext>({
    filters: {
        startRange: "",
        endRange: "",
        cursor: "",
        sortOrder: "DESC",
        pageSize: 10,
    },
    contents: [],

    /* Placeholders. Given console.logs to prevent critical smells in
     * SonarCloud.
     */
    updateFilter: (filter: FilterName, val?: FilterTypes) => {
        console.log("to please SonarCloud");
    },
    clear: () => {
        console.log("to please SonarCloud");
    },
});

/* This component handles filter state and API call refreshing for
 * tables that wish to use filtering. Additionally, this integrates
 * the Paginator hook.
 */
const FilterContext: React.FC<any> = (props: PropsWithChildren<any>) => {
    /* Filter State */
    const [startRange, setStartRange] = useState<string>("");
    const [endRange, setEndRange] = useState<string>("");
    const [cursor, setCursor] = useState<string>("");
    const [sortOrder, setSortOrder] = useState<SortOrder>("DESC");
    const [pageSize, setPageSize] = useState<PageSize>(10);

    const updateFilter = (filter: FilterName, val?: any) => {
        switch (filter) {
            case FilterName.START_RANGE:
                setStartRange(val || "");
                break;
            case FilterName.END_RANGE:
                setEndRange(val || "");
                break;
            case FilterName.CURSOR:
                setCursor(val || "");
                break;
            case FilterName.SORT_ORDER:
                setSortOrder(val || "DESC");
                break;
            case FilterName.PAGE_SIZE:
                setPageSize(val || 10);
                break;
        }
    };

    /* Just packaging it up while keeping it React-ive */
    const [filterState, setFilterState] = useState<FilterState>({
        startRange: startRange,
        endRange: endRange,
        cursor: cursor,
        sortOrder: sortOrder,
        pageSize: pageSize,
    });
    useEffect(() => {
        setFilterState({
            startRange: startRange,
            endRange: endRange,
            cursor: cursor,
            sortOrder: sortOrder,
            pageSize: pageSize,
        });
    }, [cursor, endRange, pageSize, sortOrder, startRange]);

    const clear = () => {
        setStartRange("");
        setEndRange("");
        setCursor("");
        setSortOrder("DESC");
        setPageSize(10);
    };

    /* Our API call! Updates when any of the given state variables update */
    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: getStoredOrg(),
            cursor: filterState.cursor,
            endCursor: filterState.endRange,
            pageSize: filterState.pageSize + 1, // Pulls +1 to check for next page
            sort: filterState.sortOrder,
            showFailed: false, // No plans for this to be set to true
        }
    );

    /* Pagination, baby! */
    const paginator = usePaginator(submissions, filterState);

    /* This sets the cursor to the currentIndex when currentIndex, cursors, or
     * startRange change. */
    useEffect(() => {
        // When current index is changed update the context cursor
        if (paginator.currentIndex === 1) {
            setCursor(startRange); // (should be "" by default, or a user set value)
        } else {
            const newCursor = paginator.cursors.get(paginator.currentIndex);
            if (newCursor) setCursor(newCursor);
        }
    }, [paginator.currentIndex, paginator.cursors, startRange]);

    /* This is the payload we deliver through our context provider */
    const contextPayload: ISubmissionFilterContext = {
        filters: filterState,
        contents: submissions,
        paginator: paginator,
        updateFilter: updateFilter,
        clear: clear,
    };

    return (
        <SubmissionFilterContext.Provider value={contextPayload}>
            {props.children}
        </SubmissionFilterContext.Provider>
    );
};

export default FilterContext;
