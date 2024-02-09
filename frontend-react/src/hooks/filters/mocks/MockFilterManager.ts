import { Dispatch } from "react";

import { RangeSettingsAction } from "../UseDateRange";
import { FilterManager } from "../UseFilterManager";
import { PageSettingsAction } from "../UsePages";
import { SortSettingsAction } from "../UseSortOrder";

const fakeDispatch = <T>(): Dispatch<T> => {
    return (_v: T) => void 0;
};

export const mockFilterManager: FilterManager = {
    rangeSettings: {
        from: new Date("2022-01-01").toISOString(),
        to: new Date("2022-12-31").toISOString(),
    },
    sortSettings: {
        column: "",
        order: "DESC",
        locally: false,
        localOrder: "DESC",
    },
    pageSettings: {
        size: 10,
        currentPage: 1,
    },
    updateRange: fakeDispatch<RangeSettingsAction>(),
    updateSort: fakeDispatch<SortSettingsAction>(),
    updatePage: fakeDispatch<PageSettingsAction>(),
    resetAll: () => void 0,
};
