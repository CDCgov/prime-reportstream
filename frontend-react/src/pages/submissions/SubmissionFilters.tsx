import React, { useContext, useState } from "react";
import { DatePicker, Label, Button } from "@trussworks/react-uswds";

import "./SubmissionPages.css";
import { SubmissionFilterContext } from "./FilterContext";

export enum StyleClass {
    CONTAINER = "grid-container filter-container",
    DATE_CONTAINER = "date-picker-container",
}

/* This component contains the UI for selecting query parameters.
 * When the `Apply` button is clicked, these should be updated in
 * a context wrapping this and the SubmissionTable component. The
 * table component contains the call and param passing to the API,
 * and will use the context to get these values.
 */
function SubmissionFilters() {
    const { updateStartRange, updateEndRange, clear, paginator } = useContext(
        SubmissionFilterContext
    );

    /* Local state to hold values before pushing to context. Pushing to context
     * will trigger a re-render due to the API call fetching new data. We have local
     * state to hold these so updates don't render immediately after setting a filter */
    const [localStartRange, setLocalLocalStartRange] = useState<string>();
    const [localEndRange, setLocalEndRange] = useState<string>();

    /* This workhorse function handles all Context changes with null checking */
    const pushToContext = () => {
        if (updateStartRange && localStartRange)
            updateStartRange(localStartRange);
        if (updateEndRange && localEndRange) updateEndRange(localEndRange);
    };

    /* Pushes local state to context and resets cursor to page 1 */
    const applyToContext = () => {
        pushToContext();
        if (paginator?.resetCursors) paginator.resetCursors();
        if (paginator?.changeCursor) paginator.changeCursor(1);
    };

    /* Clears context and local state values */
    const clearAll = () => {
        // Clear context
        if (clear) clear();

        // Clear local state
        setLocalLocalStartRange("");
        setLocalEndRange("");
    };

    return (
        <div data-testid="filter-container" className={StyleClass.CONTAINER}>
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
                    onChange={(val) => setLocalLocalStartRange(val)}
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
                    onChange={(val) => setLocalEndRange(val)}
                />
            </div>
            <div className={StyleClass.DATE_CONTAINER}>
                <Button onClick={() => applyToContext()} type={"button"}>
                    Filter
                </Button>
            </div>
            <div className={StyleClass.DATE_CONTAINER}>
                <Button onClick={() => clearAll()} type={"button"} outline>
                    Clear
                </Button>
            </div>
        </div>
    );
}

export default SubmissionFilters;
