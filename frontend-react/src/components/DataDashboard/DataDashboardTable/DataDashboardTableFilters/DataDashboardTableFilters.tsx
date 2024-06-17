import { Button, DateRangePicker } from "@trussworks/react-uswds";
import { FormEvent, useCallback, useRef, useState } from "react";

import "./DataDashboardTableFilters.css";

import {
    CursorActionType,
    CursorManager,
} from "../../../../hooks/filters/UseCursorManager/UseCursorManager";
import {
    FALLBACK_FROM_DATE_STRING,
    FALLBACK_FROM_STRING,
    FALLBACK_TO_DATE_STRING,
    FALLBACK_TO_STRING,
    getEndOfDay,
    RangeSettingsActionType,
} from "../../../../hooks/filters/UseDateRange/UseDateRange";
import { FilterManager } from "../../../../hooks/filters/UseFilterManager/UseFilterManager";

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
        DATE_RE.test(dateStr ?? "") && !Number.isNaN(Date.parse(dateStr ?? ""))
    );
}

/* This component contains the UI for selecting query parameters.
 * When the `Apply` button is clicked, these should be updated in
 * a context wrapping this and the SubmissionTable component. The
 * table component contains the call and param passing to the API,
 * and will use the context to get these values.
 */
function DataDashboardTableFilters({
    startDateLabel,
    endDateLabel,
    showDateHints,
    filterManager,
    cursorManager,
    onFilterClick,
}: TableFilterProps) {
    // store ISO strings to pass to FilterManager when user clicks 'Filter'
    // TODO: Remove FilterManager and CursorManager
    const [rangeFrom, setRangeFrom] = useState<string>(FALLBACK_FROM_STRING);
    const [rangeTo, setRangeTo] = useState<string>(FALLBACK_TO_STRING);
    const isFilterEnabled = Boolean(
        rangeFrom && rangeTo && rangeFrom < rangeTo,
    );
    const formRef = useRef<HTMLFormElement>(null);

    const updateRange = useCallback(
        (from: string, to: string) => {
            filterManager.updateRange({
                type: RangeSettingsActionType.RESET,
                payload: { from, to },
            });
            cursorManager?.update({
                type: CursorActionType.RESET,
                payload:
                    filterManager.sortSettings.order === "DESC" ? to : from,
            });
        },
        [cursorManager, filterManager],
    );

    /* Pushes local state to context and resets cursor to page 1 */
    const applyToFilterManager = useCallback(
        (from: string, to: string) => {
            updateRange(from, to);

            // call onFilterClick with the specified range
            if (onFilterClick) onFilterClick({ from, to });
        },
        [onFilterClick, updateRange],
    );

    /* Clears manager and local state values */
    const resetHandler = useCallback(
        (e: FormEvent) => {
            e.preventDefault();
            if (formRef.current) {
                /*
                 * can't use refs with DateRangePicker, so we go through
                 * form. we set values manaully and also manually have to
                 * invoke an input event (programatic value setting doesn't
                 * trigger). the input event will allow DateRangePicker to
                 * properly update internal state while we update the
                 * filtermanager with our manual reset values.
                 */
                const startDateEle = formRef.current.elements.namedItem(
                    "start-date",
                ) as HTMLInputElement;
                const endDateEle = formRef.current.elements.namedItem(
                    "end-date",
                ) as HTMLInputElement;

                startDateEle.value = FALLBACK_FROM_DATE_STRING;
                startDateEle.dispatchEvent(
                    new InputEvent("input", {
                        bubbles: true,
                    }),
                );
                endDateEle.value = FALLBACK_TO_DATE_STRING;
                endDateEle.dispatchEvent(
                    new InputEvent("input", {
                        bubbles: true,
                    }),
                );

                applyToFilterManager(FALLBACK_FROM_STRING, FALLBACK_TO_STRING);
            }
        },
        [applyToFilterManager],
    );

    const submitHandler = useCallback(
        (e: FormEvent) => {
            e.preventDefault();
            applyToFilterManager(rangeFrom, rangeTo);
        },
        [applyToFilterManager, rangeFrom, rangeTo],
    );

    return (
        <div data-testid="filter-container" className="filter-container">
            <form
                className="grid-row display-flex flex-align-end"
                ref={formRef}
                onSubmit={submitHandler}
                onReset={resetHandler}
            >
                <DateRangePicker
                    className={StyleClass.DATE_CONTAINER}
                    startDateLabel={startDateLabel}
                    startDateHint={showDateHints ? "mm/dd/yyyy" : ""}
                    startDatePickerProps={{
                        id: "start-date",
                        name: "start-date-picker",
                        onChange: (val?: string) => {
                            if (isValidDateString(val)) {
                                setRangeFrom(new Date(val!).toISOString());
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
                                    getEndOfDay(new Date(val!)).toISOString(),
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
                        <Button disabled={!isFilterEnabled} type={"submit"}>
                            Filter
                        </Button>
                    </div>
                    <div className={StyleClass.DATE_CONTAINER}>
                        <Button type={"reset"} name="clear-button" unstyled>
                            Clear
                        </Button>
                    </div>
                </div>
            </form>
        </div>
    );
}

export default DataDashboardTableFilters;
