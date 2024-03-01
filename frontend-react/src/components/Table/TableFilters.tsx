import {
    Button,
    DateRangePicker,
    Icon,
    Select,
    TimePicker,
    Tooltip,
} from "@trussworks/react-uswds";
import {
    Dispatch,
    FormEvent,
    SetStateAction,
    useCallback,
    useMemo,
    useRef,
    useState,
} from "react";

import styles from "./TableFilters.module.scss";
import { RSReceiver } from "../../config/endpoints/settings";
import {
    CursorActionType,
    CursorManager,
} from "../../hooks/filters/UseCursorManager";
import {
    DEFAULT_TIME,
    getEndOfDay,
    RangeSettingsActionType,
} from "../../hooks/filters/UseDateRange";
import { FilterManager } from "../../hooks/filters/UseFilterManager";

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
    receivers: { value: string; label: string }[];
    setService?: Dispatch<SetStateAction<string | undefined>>;
    showDateHints?: boolean;
    startDateLabel: string;
    initialService: RSReceiver;
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
function TableFilters({
    cursorManager,
    endDateLabel,
    filterManager,
    onFilterClick,
    receivers,
    setService,
    showDateHints,
    startDateLabel,
    initialService,
}: TableFilterProps) {
    // store ISO strings to pass to FilterManager when user clicks 'Filter'
    // TODO: Remove FilterManager and CursorManager
    const [rangeFrom, setRangeFrom] = useState<Date | undefined>(undefined);
    const [rangeTo, setRangeTo] = useState<Date | undefined>(undefined);
    const formRef = useRef<HTMLFormElement>(null);
    const [startTime, setStartTime] = useState(DEFAULT_TIME);
    const [endTime, setEndTime] = useState(DEFAULT_TIME);
    const [currentServiceSelect, setCurrentServiceSelect] = useState<string>(
        initialService?.name,
    );
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

    const filterDetails = useMemo(() => {
        // Handle edge case of JUST a receiver being selected
        if (
            currentServiceSelect &&
            !rangeFrom &&
            !rangeTo &&
            startTime === DEFAULT_TIME &&
            endTime === DEFAULT_TIME
        ) {
            return {
                isFilterDisabled: false,
                rangeFromWithTime: undefined,
                rangeToWithTime: undefined,
            };
        }

        // Early return if rangeFrom or rangeTo are not provided
        if (!rangeFrom || !rangeTo) {
            return {
                isFilterDisabled: true,
                rangeFromWithTime: undefined,
                rangeToWithTime: undefined,
            };
        }

        const [startHours, startMinutes] = startTime.split(":").map(Number);
        const [endHours, endMinutes] = endTime.split(":").map(Number);

        const rangeFromWithTime = new Date(
            rangeFrom.setHours(startHours, startMinutes, 0, 0),
        ).toISOString();
        const rangeToWithTime = new Date(
            rangeTo.setHours(endHours, endMinutes, 0, 0),
        ).toISOString();

        const isFilterDisabled = Boolean(
            new Date(rangeFromWithTime) > new Date(rangeToWithTime),
        );

        return { isFilterDisabled, rangeFromWithTime, rangeToWithTime };
    }, [currentServiceSelect, endTime, rangeFrom, rangeTo, startTime]);

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
            setRangeFrom(undefined);
            setRangeTo(undefined);
            setStartTime(DEFAULT_TIME);
            setEndTime(DEFAULT_TIME);
            setCurrentServiceSelect(initialService.name);
            setService?.(initialService.name);
            filterManager.resetAll();
        },
        [filterManager, initialService.name, reset, setService],
    );

    const submitHandler = useCallback(
        (e: FormEvent) => {
            e.preventDefault();

            setService?.(currentServiceSelect);
            if (
                filterDetails.rangeFromWithTime &&
                filterDetails.rangeToWithTime
            ) {
                applyToFilterManager(
                    filterDetails.rangeFromWithTime,
                    filterDetails.rangeToWithTime,
                );
            }
        },
        [
            applyToFilterManager,
            currentServiceSelect,
            filterDetails.rangeFromWithTime,
            filterDetails.rangeToWithTime,
            setService,
        ],
    );

    return (
        <div data-testid="filter-container" className={styles.TableFilters}>
            <form
                ref={formRef}
                onSubmit={submitHandler}
                onReset={resetHandler}
                key={reset}
                autoComplete="off"
                data-testid="filter-form"
            >
                <div className="grid-row">
                    <div className="grid-col filter-column__one">
                        <label
                            id="receiver-label"
                            data-testid="label"
                            className="usa-label"
                            htmlFor="receiver-dropdown"
                        >
                            Receiver{" "}
                            <Tooltip
                                className="fixed-tooltip text-sub"
                                position="right"
                                label={
                                    "Your connection could have multiple receivers, such as one specific to COVID."
                                }
                            >
                                <Icon.Help />
                            </Tooltip>
                        </label>
                        <Select
                            key={receivers.length}
                            id="receiver-dropdown"
                            name="receiver-dropdown"
                            onChange={(e) => {
                                setCurrentServiceSelect(e.target.value);
                            }}
                            defaultValue={currentServiceSelect}
                        >
                            {receivers?.map((receiver) => (
                                <option
                                    key={receiver.value}
                                    value={receiver.value}
                                >
                                    {receiver.value}
                                </option>
                            ))}
                        </Select>
                    </div>
                    <div className="grid-col-auto filter-column__two">
                        <DateRangePicker
                            className={StyleClass.DATE_CONTAINER}
                            startDateLabel={startDateLabel}
                            startDateHint={showDateHints ? "mm/dd/yyyy" : ""}
                            startDatePickerProps={{
                                id: "start-date",
                                name: "start-date-picker",
                                onChange: (val?: string) => {
                                    if (isValidDateString(val)) {
                                        setRangeFrom(new Date(val!));
                                    } else {
                                        setRangeFrom(undefined);
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
                                        setRangeTo(getEndOfDay(new Date(val!)));
                                    } else {
                                        setRangeTo(undefined);
                                    }
                                },
                            }}
                        />
                        <div className="grid-row">
                            <div className="grid-row flex-column">
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
                                <p className="usa-hint usa-hint__default">
                                    Default: 12:00am
                                </p>
                            </div>
                            <div className="grid-row flex-column">
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
                                <p className="usa-hint usa-hint__default">
                                    Default: 11:59pm
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="grid-col-fill filter-column__three">
                        <div className="button-container">
                            <div>
                                <Button
                                    className="margin-right-205"
                                    disabled={filterDetails.isFilterDisabled}
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
