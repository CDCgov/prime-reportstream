import { Dispatch, useCallback } from "react";

import useDateRange, { DateRange, DateRangeActionType } from "./UseDateRange";
import useSortOrder, {
    SortAction,
    SortActionType,
    SortSettings,
} from "./UseSortOrder";
import usePages, {
    PageInfo,
    PageSettingsAction,
    PageSettingsActionType,
} from "./UsePages";

export interface FilterManager {
    rangeSettings: DateRange;
    sortSettings: SortSettings;
    pageSettings: PageInfo;
    updateRange: Dispatch<any>;
    updateSort: Dispatch<SortAction>;
    updatePage: Dispatch<PageSettingsAction>;
    resetAll: () => void;
}

const useFilterManager = (): FilterManager => {
    const { settings: rangeSettings, update: updateRange } = useDateRange();
    const { settings: sortSettings, update: updateSort } = useSortOrder();
    const { settings: pageSettings, update: updatePage } = usePages();

    const resetAll = useCallback(() => {
        updateRange({ type: DateRangeActionType.RESET });
        updateSort({ type: SortActionType.RESET });
        updatePage({ type: PageSettingsActionType.RESET });
    }, [updatePage, updateRange, updateSort]);

    return {
        rangeSettings,
        sortSettings,
        pageSettings,
        updateRange,
        updateSort,
        updatePage,
        resetAll,
    };
};

export default useFilterManager;
