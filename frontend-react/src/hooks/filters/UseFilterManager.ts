import { Dispatch, useCallback } from "react";

import useDateRange, {
    RangeField,
    RangeSettings,
    RangeSettingsActionType,
} from "./UseDateRange";
import useSortOrder, {
    SortOrder,
    SortSettings,
    SortSettingsAction,
    SortSettingsActionType,
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
/** Keeps dispatch functions out of the filter shape at the network call level */
export type Filters = RangeSettings & SortSettings & PageSettings;
/** Pass in a {@link FilterManager} and receive only its filters back, no dispatch functions */
export const extractFiltersFromManager = (fm: FilterManager): Filters => ({
    ...fm.rangeSettings,
    ...fm.pageSettings,
    ...fm.sortSettings,
});

interface FilterManagerDefaults {
    sortDefaults?: Partial<SortSettings>;
    pageDefaults?: Partial<PageSettings>;
}

/* This helper can plug into your API call to allow for pagination
 * with both an ASC and DESC sort. The cursor will increment:
 *
 * history (end) -> present (start) for ASC
 * present (start) -> history (end) for DESC */
const cursorOrRange = (
    order: SortOrder,
    field: RangeField,
    cursor: string,
    range: string,
): string => {
    if (
        (order === "ASC" && field === RangeField.FROM) ||
        (order === "DESC" && field === RangeField.TO)
    ) {
        return cursor;
    }
    if (
        (order === "ASC" && field === RangeField.TO) ||
        (order === "DESC" && field === RangeField.FROM)
    ) {
        return range;
    }

    return range; // fallback to just the range value
};

const useFilterManager = (defaults?: FilterManagerDefaults): FilterManager => {
    const { settings: rangeSettings, update: updateRange } = useDateRange();
    const { settings: sortSettings, update: updateSort } = useSortOrder(
        defaults?.sortDefaults,
    );
    const { settings: pageSettings, update: updatePage } = usePages(
        defaults?.pageDefaults,
    );

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
export { cursorOrRange };
export type { FilterManagerDefaults };
