import useDateRange, { DateRange } from "./UseDateRange";
import useSortOrder, { Sort } from "./UseSortOrder";
import usePageSize, { PageSize } from "./UsePageSize";

export interface FilterState {
    range: DateRange;
    sort: Sort;
    pageSize: PageSize;
}

export interface FilterManager extends FilterState {
    clearAll: () => void;
}

const useFilterManager = (): FilterManager => {
    const range = useDateRange();
    const sort = useSortOrder();
    const pageSize = usePageSize();

    const clearAll = () => {
        range.controller.reset();
        sort.reset();
        pageSize.reset();
    };

    return {
        range,
        sort,
        pageSize,
        clearAll,
    };
};

export default useFilterManager;
