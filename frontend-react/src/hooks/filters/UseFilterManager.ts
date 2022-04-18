import { Dispatch, useCallback } from "react";

import useDateRange, {
    RangeSettings,
    RangeSettingsActionType,
} from "./UseDateRange";
import useSortOrder, {
    SortSettingsAction,
    SortSettingsActionType,
    SortSettings,
} from "./UseSortOrder";
import usePages, {
    PageSettings,
    PageSettingsAction,
    PageSettingsActionType,
} from "./UsePages";

export interface FilterManager {
    rangeSettings: RangeSettings;
    sortSettings: SortSettings;
    pageSettings: PageSettings;
    updateRange: Dispatch<any>;
    updateSort: Dispatch<SortSettingsAction>;
    updatePage: Dispatch<PageSettingsAction>;
    resetAll: () => void;
}

const useFilterManager = (): FilterManager => {
    const { settings: rangeSettings, update: updateRange } = useDateRange();
    const { settings: sortSettings, update: updateSort } = useSortOrder();
    const { settings: pageSettings, update: updatePage } = usePages();

    const resetAll = useCallback(() => {
        updateRange({ type: RangeSettingsActionType.RESET });
        updateSort({ type: SortSettingsActionType.RESET });
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
