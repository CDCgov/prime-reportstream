import React, { useState } from "react";
import { Button, DateRangePicker } from "@trussworks/react-uswds";

import "./TableFilters.css";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import {
    FALLBACK_FROM,
    FALLBACK_TO,
    getEndOfDay,
    RangeSettingsActionType,
} from "../../hooks/filters/UseDateRange";

export enum StyleClass {
    CONTAINER = "filter-container",
    DATE_CONTAINER = "date-picker-container tablet:grid-col",
}

export enum TableFilterDateLabel {
    START_DATE = "From (Start Range):",
    END_DATE = "Until (End Range):",
}

interface TableFilterProps {
    startDateLabel: string;
    endDateLabel: string;
    showDateHints?: boolean;
    filterManager: FilterManager;
    cursorManager?: CursorManager;
    onFilterClick?: ({ from, to }: { from: string; to: string }) => void;
}

// using a regex to check for format because of different browsers' implementations of Date
// e.g.:
//   new Date('11') in Chrome --> Date representation of 11/01/2001
//   new Date('11') in Firefox --> Invalid Date
const DATE_RE = /^[0-9]{1,2}\/[0-9]{1,2}\/[0-9]{2,4}$/;

export function isValidDateString(dateStr?: string) {
    // need to check for value format (mm/dd/yyyy) and date validity (no 99/99/9999)
    return (
        DATE_RE.test(dateStr || "") && !Number.isNaN(Date.parse(dateStr || ""))
    );
}

/* This component contains the UI for selecting query parameters.
 * When the `Apply` button is clicked, these should be updated in
 * a context wrapping this and the SubmissionTable component. The
 * table component contains the call and param passing to the API,
 * and will use the context to get these values.
 */
function TableFilters({
    startDateLabel,
    endDateLabel,
    showDateHints,
    filterManager,
    cursorManager,
    onFilterClick,
}: TableFilterProps) {
    // store ISO strings to pass to FilterManager when user clicks 'Filter'
    // TODO: Remove FilterManager and CursorManager
    const [rangeFrom, setRangeFrom] = useState<string>(FALLBACK_FROM);
    const [rangeTo, setRangeTo] = useState<string>(FALLBACK_TO);
    const isFilterEnabled = Boolean(
        rangeFrom && rangeTo && rangeFrom < rangeTo,
    );

    const updateRange = () => {
        filterManager.updateRange({
            type: RangeSettingsActionType.RESET,
            payload: { from: rangeFrom, to: rangeTo },
        });
        cursorManager &&
            cursorManager.update({
                type: CursorActionType.RESET,
                payload:
                    filterManager.sortSettings.order === "DESC"
                        ? rangeTo
                        : rangeFrom,
            });
    };

    /* Pushes local state to context and resets cursor to page 1 */
    const applyToFilterManager = () => {
        updateRange();

        // call onFilterClick with the specified range
        if (onFilterClick) onFilterClick({ from: rangeFrom, to: rangeTo });
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
                    startDateLabel={startDateLabel}
                    startDateHint={showDateHints ? "mm/dd/yyyy" : ""}
                    startDatePickerProps={{
                        id: "start-date",
                        name: "start-date-picker",
                        onChange: (val?: string) => {
                            if (isValidDateString(val)) {
                                setRangeFrom(new Date(val!!).toISOString());
                            } else {
                                setRangeFrom("");
                            }
                        },
                        defaultValue: rangeFrom,
                    }}
                    endDateLabel={endDateLabel}
                    endDateHint={showDateHints ? "mm/dd/yyyy" : ""}
                    endDatePickerProps={{
                        id: "end-date",
                        name: "end-date-picker",
                        onChange: (val?: string) => {
                            if (isValidDateString(val)) {
                                setRangeTo(
                                    getEndOfDay(new Date(val!!)).toISOString(),
                                );
                            } else {
                                setRangeTo("");
                            }
                        },
                        defaultValue: rangeTo,
                    }}
                />
                <div className="button-container">
                    <div className={StyleClass.DATE_CONTAINER}>
                        <Button
                            disabled={!isFilterEnabled}
                            onClick={() => applyToFilterManager()}
                            type={"button"}
                        >
                            Filter
                        </Button>
                    </div>
                    <div className={StyleClass.DATE_CONTAINER}>
                        <Button
                            onClick={clearAll}
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
