import React, { useState } from "react";
import { Button, DateRangePicker } from "@trussworks/react-uswds";

import "../../pages/submissions/SubmissionPages.css";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import {
    FALLBACK_FROM,
    FALLBACK_TO,
    RangeSettingsActionType,
} from "../../hooks/filters/UseDateRange";

export enum StyleClass {
    CONTAINER = "grid-container filter-container",
    DATE_CONTAINER = "date-picker-container tablet:grid-col",
}

interface SubmissionFilterProps {
    filterManager: FilterManager;
    cursorManager?: CursorManager;
    onFilterClick?: ({ from, to }: { from: string; to: string }) => void;
}

/* This helper ensures start range values are inclusive
 * of the day set in the date picker. */
const inclusiveDateString = (originalDate: string) => {
    let inclusiveDateDate = new Date(originalDate);
    return new Date(
        inclusiveDateDate.setUTCHours(23, 59, 59, 999)
    ).toISOString();
};

/* This component contains the UI for selecting query parameters.
 * When the `Apply` button is clicked, these should be updated in
 * a context wrapping this and the SubmissionTable component. The
 * table component contains the call and param passing to the API,
 * and will use the context to get these values.
 */
function TableFilters({
    filterManager,
    cursorManager,
    onFilterClick,
}: SubmissionFilterProps) {
    /* Local state to hold values before pushing to context. Pushing to context
     * will trigger a re-render due to the API call fetching new data. We have local
     * state to hold these so updates don't render immediately after setting a filter */
    const [rangeFrom, setRangeFrom] = useState<string>(FALLBACK_FROM);
    const [rangeTo, setRangeTo] = useState<string>(FALLBACK_TO);

    let from = new Date(rangeFrom).toISOString();
    let to = inclusiveDateString(rangeTo);

    const updateRange = () => {
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
    };

    /* Pushes local state to context and resets cursor to page 1 */
    const applyToFilterManager = () => {
        updateRange();

        // call onFilterClick with the specified range
        if (onFilterClick) onFilterClick({ from, to });
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
