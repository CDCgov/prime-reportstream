import { Dispatch } from "react";

import useDateRange, { DateRange } from "./UseDateRange";
import useSortOrder, { SortAction, SortSettings } from "./UseSortOrder";
import usePages, { PageInfo, PageSettingsAction } from "./UsePages";

export interface FilterManager {
    rangeSettings: DateRange;
    sortSettings: SortSettings;
    pageSettings: PageInfo;
    updateRange: Dispatch<any>;
    updateSort: Dispatch<SortAction>;
    updatePage: Dispatch<PageSettingsAction>;
}

const useFilterManager = (): FilterManager => {
    const { settings: rangeSettings, update: updateRange } = useDateRange();
    const { settings: sortSettings, update: updateSort } = useSortOrder();
    const { settings: pageSettings, update: updatePage } = usePages();

    return {
        rangeSettings,
        sortSettings,
        pageSettings,
        updateRange,
        updateSort,
        updatePage,
    };
};

export default useFilterManager;
