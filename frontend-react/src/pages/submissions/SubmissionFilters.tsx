import React, { useContext, useState } from "react";
import { DatePicker, Label } from "@trussworks/react-uswds";

import "./SubmissionPages.css";
import { SubmissionFilterContext } from "./FilterContext";

export enum StyleClass {
    CONTAINER = "grid-container filter-container",
    DATE_CONTAINER = "date-picker-container",
    APPLY_BUTTON = "apply-button",
}

export enum FieldNames {
    START_RANGE = "start-range",
    END_RANGE = "end-range",
}

const fieldError = (field: FieldNames) =>
    console.error(`'${field}' has no update function.`);
// const valueError = (field: FieldNames) =>
//     console.error(`'${field}' was given no value.`);
/*
 * This component contains the UI for selecting query parameters.
 * When the `Apply` button is clicked, these should be updated in
 * a context wrapping this and the SubmissionTable component. The
 * table component contains the call and param passing to the API,
 * and will use the context to get these values.
 */
function SubmissionFilters() {
    const { filters, updateStartRange, updateEndRange, paginator } = useContext(
        SubmissionFilterContext
    );
    const [startRange, setStartRange] = useState<string>();
    const [endRange, setEndRange] = useState<string>();

    const applyToContext = () => {
        handleValueStateChange(startRange, FieldNames.START_RANGE);
        handleValueStateChange(endRange, FieldNames.END_RANGE);
        if (paginator?.resetCursors) paginator.resetCursors();
        if (paginator?.changeCursor) paginator.changeCursor(1);
    };

    /* This workhorse function handles all Context changes with null checking */
    const handleValueStateChange = (
        val: string | undefined,
        property: FieldNames
    ) => {
        switch (property) {
            case FieldNames.START_RANGE:
                /* Catches null updater */
                if (!updateStartRange) {
                    fieldError(FieldNames.START_RANGE);
                    return;
                }
                if (val) updateStartRange(new Date(val).toISOString());
                break;
            case FieldNames.END_RANGE:
                /* Catches null updater */
                if (!updateEndRange) {
                    fieldError(FieldNames.END_RANGE);
                    return;
                }
                if (val) updateEndRange(new Date(val).toISOString());
                break;
        }
    };

    return (
        <div data-testid="filter-container" className={StyleClass.CONTAINER}>
            <div className={StyleClass.DATE_CONTAINER}>
                <Label htmlFor="start-date" id="start-date-label">
                    Submitted (Start Range)
                </Label>
                <DatePicker
                    id="start-date"
                    name="start-date-picker"
                    placeholder="Start Date"
                    value={filters.startRange}
                    onChange={(val) => setStartRange(val)}
                />
            </div>
            <div className={StyleClass.DATE_CONTAINER}>
                <Label htmlFor="start-date" id="start-date-label">
                    Submitted (End Range)
                </Label>
                <DatePicker
                    id="end-date"
                    name="end-date-picker"
                    placeholder="End Date"
                    value={filters.endRange}
                    onChange={(val) => setEndRange(val)}
                />
            </div>
            <div className={StyleClass.DATE_CONTAINER}>
                <button
                    className={StyleClass.APPLY_BUTTON}
                    onClick={() => applyToContext()}
                >
                    <span className="usa-link">Apply</span>
                </button>
            </div>
        </div>
    );
}

export default SubmissionFilters;
