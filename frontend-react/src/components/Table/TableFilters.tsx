import React, { useState } from "react";
import { Button, DateRangePicker } from "@trussworks/react-uswds";

import "../../pages/submissions/SubmissionPages.css";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import {
    FALLBACK_TO,
    FALLBACK_FROM,
    RangeSettingsActionType,
} from "../../hooks/filters/UseDateRange";

export enum StyleClass {
    CONTAINER = "grid-container filter-container",
    DATE_CONTAINER = "date-picker-container tablet:grid-col",
}

export enum FilterName {
    START_RANGE = "start-range",
    END_RANGE = "end-range",
    CURSOR = "cursor",
    SORT_ORDER = "sort-order",
    PAGE_SIZE = "page-size",
}

interface SubmissionFilterProps {
    filterManager: FilterManager;
    cursorManager?: CursorManager;
}

/* This helper ensures start range values are inclusive
 * of the day set in the date picker. */
const inclusiveDateString = (originalDate: string) => {
    return `${originalDate} 23:59:59 GMT`;
};

/* This component contains the UI for selecting query parameters.
 * When the `Apply` button is clicked, these should be updated in
 * a context wrapping this and the SubmissionTable component. The
 * table component contains the call and param passing to the API,
 * and will use the context to get these values.
 */
function TableFilters({ filterManager, cursorManager }: SubmissionFilterProps) {
    /* Local state to hold values before pushing to context. Pushing to context
     * will trigger a re-render due to the API call fetching new data. We have local
     * state to hold these so updates don't render immediately after setting a filter */
    const [rangeFrom, setRangeFrom] = useState<string>(FALLBACK_FROM);
    const [rangeTo, setRangeTo] = useState<string>(FALLBACK_TO);

    const updateRange = () => {
        try {
            const from = new Date(rangeFrom).toISOString();
            const to = new Date(inclusiveDateString(rangeTo)).toISOString();
            filterManager.updateRange({
                type: RangeSettingsActionType.RESET,
                payload: { from, to },
            });
            cursorManager &&
                cursorManager.update({
                    type: CursorActionType.RESET,
                    payload:
                        filterManager.sortSettings.order === "DESC" ? to : from,
                });
        } catch (e) {
            console.warn(e);
        }
    };

    /* Pushes local state to context and resets cursor to page 1 */
    const applyToFilterManager = () => {
        updateRange();
        // Future functions to update filters here
    };

    /* Clears manager and local state values */
    const clearAll = () => {
        // Clears manager state
        filterManager.resetAll();
        cursorManager && cursorManager.update({ type: CursorActionType.RESET });

        // Clear local state
        setRangeFrom(FALLBACK_FROM);
        setRangeTo(FALLBACK_TO);
    };

    return (
        <div data-testid="filter-container" className={StyleClass.CONTAINER}>
            <div className="grid-row display-flex flex-align-end">
                <DateRangePicker
                    className={StyleClass.DATE_CONTAINER}
                    startDateLabel="From (Start Range):"
                    startDatePickerProps={{
                        id: "start-date",
                        name: "start-date-picker",
                        onChange: (val?: string) => {
                            val
                                ? setRangeFrom(val)
                                : console.warn("Start Range is undefined");
                        },
                    }}
                    endDateLabel="Until (End Range):"
                    endDatePickerProps={{
                        id: "end-date",
                        name: "end-date-picker",
                        onChange: (val?: string) => {
                            val
                                ? setRangeTo(val)
                                : console.warn("Start Range is undefined");
                        },
                    }}
                />
                <div className="button-container">
                    <div className={StyleClass.DATE_CONTAINER}>
                        <Button
                            onClick={() => applyToFilterManager()}
                            type={"button"}
                        >
                            Filter
                        </Button>
                    </div>
                    <div className={StyleClass.DATE_CONTAINER}>
                        <Button
                            onClick={() => clearAll()}
                            type={"button"}
                            name="clear-button"
                            unstyled
                        >
                            Clear
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default TableFilters;
export { inclusiveDateString };
