import useDateRange, { DateRange } from "./UseDateRange";
import useSortOrder, { Sort } from "./UseSortOrder";
import usePageSize, { PageSize } from "./UsePageSize";

export interface FilterManager extends DateRange, Sort, PageSize {
    resetAll: () => void;
}

const useFilterManager = (): FilterManager => {
    const { startRange, endRange, setRange, resetRange } = useDateRange();
    const { order, column, setSort, resetSort } = useSortOrder();
    const { count, setCount, resetCount } = usePageSize();

    const resetAll = () => {
        resetRange();
        resetSort();
        resetCount();
    };

    return {
        startRange,
        endRange,
        order,
        column,
        count,
        setRange,
        setSort,
        setCount,
        resetRange,
        resetSort,
        resetCount,
        resetAll,
    };
};

export default useFilterManager;
