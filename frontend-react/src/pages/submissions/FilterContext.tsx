import React, {
    createContext,
    PropsWithChildren,
    useContext,
    useState,
} from "react";
import { useResource } from "rest-hooks";

import usePaginator from "../../utils/UsePaginator";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { GlobalContext } from "../../components/GlobalContextProvider";

type SortOrder = "ASC" | "DESC";
type PageSize = 10 | 25 | 50 | 100;
type StateUpdate<T> = (val: T) => void;

export interface FilterState {
    startRange: string;
    endRange: string;
    cursor: string;
    sortOrder: SortOrder;
    pageSize: PageSize;
}

interface PaginationController {
    hasPrev: boolean;
    hasNext: boolean;
    currentIndex: number;
    changeCursor?: (cursorIndex: number) => void;
    pageCount?: () => number;
}

interface ISubmissionFilterContext {
    filters: FilterState;
    paginator: PaginationController;
    contents: SubmissionsResource[];
    updateStartRange?: StateUpdate<string>;
    updateEndRange?: StateUpdate<string>;
    updateCursor?: StateUpdate<string>;
    updateSortOrder?: StateUpdate<SortOrder>;
    updatePageSize?: StateUpdate<PageSize>;
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
    paginator: {
        hasPrev: false,
        hasNext: false,
        currentIndex: 0,
    },
    contents: [],
});

/*
 * This component handles a pseudo-global state for the Submission
 * components; primarily linking SubmissionTable and
 * SubmissionFilters. This is much friendlier than callback functions
 * and piping props!
 */
const FilterContext: React.FC<any> = (props: PropsWithChildren<any>) => {
    const globalState = useContext(GlobalContext);

    /* Filter State */
    const [startRange, setStartRange] = useState<string>("");
    const [endRange, setEndRange] = useState<string>("");
    const [cursor, setCursor] = useState<string>("");
    const [sortOrder, setSortOrder] = useState<SortOrder>("DESC");
    const [pageSize, setPageSize] = useState<PageSize>(10);
    const updateStartRange = (val: string) => setStartRange(val);
    const updateEndRange = (val: string) => setEndRange(val);
    const updateCursor = (val: string) => setCursor(val);
    const updateSortOrder = (val: SortOrder) => setSortOrder(val);
    const updatePageSize = (val: PageSize) => setPageSize(val);

    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: globalState.state.organization,
            cursor: cursor,
            endCursor: endRange,
            pageSize: pageSize + 1, // Pulls +1 to check for next page
            sort: sortOrder,
            showFailed: false, // No plans for this to be set to true
        }
    );

    const filters: FilterState = {
        startRange: startRange,
        endRange: endRange,
        cursor: cursor,
        sortOrder: sortOrder,
        pageSize: pageSize,
    };

    const paginator = usePaginator(submissions, filters, updateCursor);

    /* This is the payload we deliver through our context provider */
    const contextPayload: ISubmissionFilterContext = {
        filters: filters,
        paginator: paginator,
        contents: submissions,
        updateStartRange: updateStartRange,
        updateEndRange: updateEndRange,
        updateCursor: updateCursor,
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
