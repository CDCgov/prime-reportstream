import React, { useState } from "react";
import { Button, DatePicker, Label } from "@trussworks/react-uswds";

import "../../pages/submissions/SubmissionPages.css";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import { RangeSettingsActionType } from "../../hooks/filters/UseDateRange";

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
    cursorManager: CursorManager;
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
    const FALLBACK_LOCAL_START = "2999-12-31";
    const FALLBACK_LOCAL_END = "2020-01-01";

    /* Local state to hold values before pushing to context. Pushing to context
     * will trigger a re-render due to the API call fetching new data. We have local
     * state to hold these so updates don't render immediately after setting a filter */
    const [localStartRange, setLocalStartRange] =
        useState<string>(FALLBACK_LOCAL_START);
    const [localEndRange, setLocalEndRange] =
        useState<string>(FALLBACK_LOCAL_END);

    const updateRange = () => {
        if (localStartRange && localEndRange) {
            filterManager.updateRange({
                type: RangeSettingsActionType.RESET,
                payload: {
                    start: new Date(
                        inclusiveDateString(localStartRange)
                    ).toISOString(),
                    end: new Date(localEndRange).toISOString(),
                },
            });
            cursorManager.update({
                type: CursorActionType.RESET,
                payload: new Date(
                    inclusiveDateString(localStartRange)
                ).toISOString(),
            });
        } else if (localStartRange && !localEndRange) {
            filterManager.updateRange({
                type: RangeSettingsActionType.UPDATE_START,
                payload: {
                    start: new Date(
                        inclusiveDateString(localStartRange)
                    ).toISOString(),
                },
            });
            cursorManager.update({
                type: CursorActionType.RESET,
                payload: new Date(
                    inclusiveDateString(localStartRange)
                ).toISOString(),
            });
        } else if (!localStartRange && localEndRange) {
            filterManager.updateRange({
                type: RangeSettingsActionType.UPDATE_END,
                payload: {
                    end: new Date(localEndRange).toISOString(),
                },
            });
            cursorManager.update({
                type: CursorActionType.RESET,
                payload: new Date(localEndRange).toISOString(),
            });
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
        cursorManager.update({ type: CursorActionType.RESET });

        // Clear local state
        setLocalStartRange(FALLBACK_LOCAL_START);
        setLocalEndRange(FALLBACK_LOCAL_END);
    };

    return (
        <div data-testid="filter-container" className={StyleClass.CONTAINER}>
            <div className="grid-row display-flex flex-align-end">
                <div className={StyleClass.DATE_CONTAINER}>
                    <Label htmlFor="start-date" id="start-date-label">
                        Submitted (Start Range)
                    </Label>
                    {/* BUG: Despite value being set to the local state, clearing it
                     does not actually clear the displayed value. */}
                    <DatePicker
                        id="start-date"
                        name="start-date-picker"
                        placeholder="Start Date"
                        value={localStartRange}
                        onChange={(val) =>
                            val
                                ? setLocalStartRange(val)
                                : console.log("StartRange is undefined")
                        }
                    />
                </div>
                <div className={StyleClass.DATE_CONTAINER}>
                    <Label htmlFor="start-date" id="start-date-label">
                        Submitted (End Range)
                    </Label>
                    {/* BUG: Despite value being set to the local state, clearing it
                     does not actually clear the displayed value. */}
                    <DatePicker
                        id="end-date"
                        name="end-date-picker"
                        placeholder="End Date"
                        value={localEndRange}
                        onChange={(val) =>
                            val
                                ? setLocalEndRange(val)
                                : console.log("EndRange is undefined")
                        }
                    />
                </div>
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
