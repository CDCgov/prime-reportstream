import React, { useContext } from "react";
import { DatePicker, Label } from "@trussworks/react-uswds";

import "./Submissions.css";
import { SubmissionFilterContext } from "./SubmissionContext";

export enum StyleClass {
    CONTAINER = "grid-container filter-container",
    DATE_CONTAINER = "date-picker-container",
}

export enum FieldNames {
    START_RANGE = "start-range",
    END_RANGE = "end-range",
}

const fieldError = (field: FieldNames) =>
    console.error(`'${field}' has no update function.`);
const valueError = (field: FieldNames) =>
    console.error(`'${field}' cannot update; given no value.`);
/*
 * This component contains the UI for selecting query parameters.
 * When the `Apply` button is clicked, these should be updated in
 * a context wrapping this and the SubmissionsTable component. The
 * table component contains the call and param passing to the API,
 * and will use the context to get these values.
 */
function SubmissionFilters() {
    const { filters, updateStartRange, updateEndRange } = useContext(
        SubmissionFilterContext
    );
    const handleChange = (val: string | undefined, property: FieldNames) => {
        /* Catch null values */
        if (!val) {
            valueError(property);
            return;
        }
        switch (property) {
            case FieldNames.START_RANGE:
                /* Catches null updater */
                if (!updateStartRange) {
                    fieldError(FieldNames.START_RANGE);
                    return;
                }
                updateStartRange(val);
                break;
            case FieldNames.END_RANGE:
                /* Catches null updater */
                if (!updateEndRange) {
                    fieldError(FieldNames.END_RANGE);
                    return;
                }
                updateEndRange(val);
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
                    onChange={(val) => {
                        handleChange(val, FieldNames.START_RANGE);
                    }}
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
                    onChange={(val) => {
                        handleChange(val, FieldNames.END_RANGE);
                    }}
                />
            </div>
        </div>
    );
}

export default SubmissionFilters;
