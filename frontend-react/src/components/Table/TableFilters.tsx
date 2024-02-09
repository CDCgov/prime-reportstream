import React, {
    Dispatch,
    FormEvent,
    SetStateAction,
    useCallback,
    useRef,
    useState,
} from "react";
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
    FALLBACK_FROM_STRING,
    FALLBACK_TO_STRING,
    getEndOfDay,
    RangeSettingsActionType,
    DEFAULT_TIME,
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
    cursorManager?: CursorManager;
    endDateLabel: string;
    filterManager: FilterManager;
    onFilterClick?: ({ from, to }: { from: string; to: string }) => void;
    receivers?: { value: string; label: string }[];
    setService: Dispatch<SetStateAction<string | undefined>>;
    showDateHints?: boolean;
    startDateLabel: string;
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
    cursorManager,
    endDateLabel,
    filterManager,
    onFilterClick,
    receivers,
    setService,
    showDateHints,
    startDateLabel,
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
    const [startTime, setStartTime] = useState(DEFAULT_TIME);
    const [endTime, setEndTime] = useState(DEFAULT_TIME);
    const [currentServiceSelect, setCurrentServiceSelect] = useState<
        string | undefined
    >(undefined);
    const [reset, setReset] = useState(0);

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
            setReset(reset + 1);
            setRangeFrom(new Date(FALLBACK_FROM_STRING));
            setRangeTo(new Date(FALLBACK_TO_STRING));
            setStartTime(DEFAULT_TIME);
            setEndTime(DEFAULT_TIME);
        },
        [reset],
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
            setService(currentServiceSelect);
            applyToFilterManager(rangeFromWithTime, rangeToWithTime);
        },
        [
            applyToFilterManager,
            currentServiceSelect,
            endTime,
            rangeFrom,
            rangeTo,
            setService,
            startTime,
        ],
    );

    return (
        <div data-testid="filter-container" className={styles.TableFilters}>
            <form
                className="grid-row display-flex flex-align-end"
                ref={formRef}
                onSubmit={submitHandler}
                onReset={resetHandler}
                key={reset}
                autoComplete="off"
            >
                <div>
                    <div className="grid-row">
                        <div className="grid-col-3 filter-column__one">
                            <label
                                id="start-date-label"
                                data-testid="label"
                                className="usa-label"
                                htmlFor="input-ComboBox"
                            >
                                Receiver
                            </label>
                            <div>
                                <p className="usa-hint" id="start-date-hint">
                                    Your connection could have multiple
                                    receivers, such as one specific to COVID.
                                </p>
                            </div>
                            {receivers && (
                                <ComboBox
                                    key={receivers.length}
                                    id="input-ComboBox"
                                    name="input-ComboBox"
                                    options={receivers}
                                    onChange={(selection) => {
                                        setCurrentServiceSelect(selection);
                                    }}
                                />
                            )}
                        </div>
                        <div className="grid-col filter-column__two">
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
                                }}
                            />
                            <div className="grid-row flex-no-wrap">
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
                                    <p className="usa-hint margin-top-1">
                                        Default: 12:00am
                                    </p>
                                </div>
                                <div className="grid-row">
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
                                    <p className="usa-hint margin-top-1">
                                        Default: 11:59pm
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div className="grid-col-2 filter-column__three">
                            <div className="button-container">
                                <Button
                                    disabled={!isFilterEnabled}
                                    type={"submit"}
                                >
                                    Apply
                                </Button>
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
            </form>
        </div>
    );
}

export default TableFilters;
