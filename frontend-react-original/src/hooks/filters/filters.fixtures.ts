import { Dispatch } from "react";

import { CursorManager } from "./UseCursorManager/UseCursorManager";
import { RangeSettingsAction } from "./UseDateRange/UseDateRange";
import { FilterManager } from "./UseFilterManager/UseFilterManager";
import { PageSettingsAction } from "./UsePages/UsePages";
import { SortSettingsAction } from "./UseSortOrder/UseSortOrder";

export const cursorManagerFixture: CursorManager = {
    cursors: {
        current: "",
        next: "",
        history: [],
    },
    hasPrev: false,
    hasNext: false,

    update: () => void 0,
};

const fakeDispatch = <T>(): Dispatch<T> => {
    return (_v: T) => void 0;
};

export const filterManagerFixture: FilterManager = {
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
