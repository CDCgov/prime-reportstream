import { Dispatch } from "react";

import { FilterManager } from "../UseFilterManager";
import { RangeSettingsAction } from "../UseDateRange";
import { SortSettingsAction } from "../UseSortOrder";
import { PageSettingsAction } from "../UsePages";

const fakeDispatch = <T>(): Dispatch<T> => {
    return (v: T) => console.log(v);
};

export const mockFilterManager = {
    rangeSettings: {
        from: new Date("2022-01-01").toISOString(),
        to: new Date("2022-12-31").toISOString(),
    },
    sortSettings: {
        column: "",
        order: "DESC",
    },
    pageSettings: {
        size: 10,
        currentPage: 1,
    },
    updateRange: fakeDispatch<RangeSettingsAction>(),
    updateSort: fakeDispatch<SortSettingsAction>(),
    updatePage: fakeDispatch<PageSettingsAction>(),
} as FilterManager;
