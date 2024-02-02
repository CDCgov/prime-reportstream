import React, { FormEvent, useCallback, useRef, useState } from "react";
import {
    Button,
    ComboBox,
    DateRangePicker,
    TimePicker,
} from "@trussworks/react-uswds";

import { FilterManager } from "../../hooks/filters/UseFilterManager";
import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import {
    FALLBACK_FROM,
    FALLBACK_FROM_STRING,
    FALLBACK_TO,
    FALLBACK_TO_STRING,
    getEndOfDay,
    RangeSettingsActionType,
} from "../../hooks/filters/UseDateRange";

import styles from "./TableFilters.module.scss";

export enum StyleClass {
    CONTAINER = "filter-container",
    DATE_CONTAINER = "date-picker-container tablet:grid-col",
}

export enum TableFilterDateLabel {
    START_DATE = "From",
    END_DATE = "To",
}

interface TableFilterProps {
    receivers: { value: string; label: string }[];
    startDateLabel: string;
    endDateLabel: string;
    showDateHints?: boolean;
    filterManager: FilterManager;
    handleSetActiveService: (s: string) => void;
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
    receivers,
    startDateLabel,
    endDateLabel,
    showDateHints,
    filterManager,
    handleSetActiveService,
    cursorManager,
    onFilterClick,
}: TableFilterProps) {
    // store ISO strings to pass to FilterManager when user clicks 'Filter'
    // TODO: Remove FilterManager and CursorManager
    const [rangeFrom, setRangeFrom] = useState<Date>(
        new Date(FALLBACK_FROM_STRING),
    );
    const [rangeTo, setRangeTo] = useState<Date>(new Date(FALLBACK_TO_STRING));
    const isFilterEnabled = Boolean(
        rangeFrom && rangeTo && rangeFrom < rangeTo,
    );
    const formRef = useRef<HTMLFormElement>(null);
    const [startTime, setStartTime] = useState("0:0");
    const [endTime, setEndTime] = useState("0:0");

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

                startDateEle.value = FALLBACK_FROM_STRING;
                startDateEle.dispatchEvent(
                    new InputEvent("input", {
                        bubbles: true,
                    }),
                );
                endDateEle.value = FALLBACK_TO_STRING;
                endDateEle.dispatchEvent(
                    new InputEvent("input", {
                        bubbles: true,
                    }),
                );

                applyToFilterManager(FALLBACK_FROM, FALLBACK_TO);
            }
        },
        [applyToFilterManager],
    );

    const submitHandler = useCallback(
        (e: FormEvent) => {
            e.preventDefault();
            const [startHours, startMinutes] = startTime.split(":").map(Number);
            const [endHours, endMinutes] = endTime.split(":").map(Number);
            const rangeFromWithTime = new Date(
                rangeFrom.setHours(startHours, startMinutes, 0, 0),
            ).toISOString();
            const rangeToWithTime = new Date(
                rangeTo.setHours(endHours, endMinutes, 0, 0),
            ).toISOString();
            applyToFilterManager(rangeFromWithTime, rangeToWithTime);
        },
        [applyToFilterManager, endTime, rangeFrom, rangeTo, startTime],
    );

    return (
        <div data-testid="filter-container" className={styles.TableFilters}>
            <form
                className="grid-row display-flex flex-align-end"
                ref={formRef}
                onSubmit={submitHandler}
                onReset={resetHandler}
            >
                <div>
                    <div className="grid-row">
                        <div className="grid-col-4 filter-column__one">
                            <label
                                id="start-date-label"
                                data-testid="label"
                                className="usa-label"
                                htmlFor="input-ComboBox"
                            >
                                Receiver
                            </label>
                            <div className="usa-hint" id="start-date-hint">
                                Your connection could have multiple receivers,
                                such as one specific to COVID.
                            </div>
                            <ComboBox
                                key={receivers.length}
                                id="input-ComboBox"
                                name="input-ComboBox"
                                options={receivers}
                                onChange={(selection) => {
                                    if (selection)
                                        handleSetActiveService(selection);
                                }}
                            />
                        </div>
                        <div className="grid-col-6 filter-column__two">
                            <DateRangePicker
                                className={StyleClass.DATE_CONTAINER}
                                startDateLabel={startDateLabel}
                                startDateHint={
                                    showDateHints ? "mm/dd/yyyy" : ""
                                }
                                startDatePickerProps={{
                                    id: "start-date",
                                    name: "start-date-picker",
                                    onChange: (val?: string) => {
                                        if (isValidDateString(val)) {
                                            setRangeFrom(new Date(val!!));
                                        }
                                    },
                                    defaultValue: rangeFrom.toISOString(),
                                }}
                                endDateLabel={endDateLabel}
                                endDateHint={showDateHints ? "mm/dd/yyyy" : ""}
                                endDatePickerProps={{
                                    id: "end-date",
                                    name: "end-date-picker",
                                    onChange: (val?: string) => {
                                        if (isValidDateString(val)) {
                                            setRangeTo(
                                                getEndOfDay(new Date(val!!)),
                                            );
                                        }
                                    },
                                    defaultValue: rangeTo.toISOString(),
                                }}
                            />
                            <div className="grid-row">
                                <TimePicker
                                    hint="hh:mm"
                                    id="start-time"
                                    label="Start time"
                                    name="start-time"
                                    step={1}
                                    onChange={(input) => {
                                        if (input) {
                                            setStartTime(input);
                                        } else {
                                            setStartTime("0:0");
                                        }
                                    }}
                                />
                                <TimePicker
                                    hint="hh:mm"
                                    id="end-time"
                                    label="End time"
                                    name="end-time"
                                    step={1}
                                    onChange={(input) => {
                                        if (input) {
                                            setEndTime(input);
                                        } else {
                                            setEndTime("0:0");
                                        }
                                    }}
                                />
                            </div>
                        </div>
                        <div className="grid-col-2 filter-column__three">
                            <div className="button-container">
                                <div className={StyleClass.DATE_CONTAINER}>
                                    <Button
                                        disabled={!isFilterEnabled}
                                        type={"submit"}
                                    >
                                        Apply
                                    </Button>
                                </div>
                                <div className={StyleClass.DATE_CONTAINER}>
                                    <Button
                                        type={"reset"}
                                        name="clear-button"
                                        unstyled
                                    >
                                        Reset
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    );
}

export default TableFilters;
