import { Button, DateRangePicker, Icon, Select, TimePicker, Tooltip } from "@trussworks/react-uswds";
import { format, isValid, parse } from "date-fns";
import { Dispatch, FormEvent, SetStateAction, useCallback, useEffect, useMemo, useRef, useState } from "react";

import styles from "./TableFilters.module.scss";
import TableFilterSearch from "./TableFilterSearch";
import TableFilterStatus, { TableFilterData } from "./TableFilterStatus";
import { CursorActionType, CursorManager } from "../../hooks/filters/UseCursorManager/UseCursorManager";
import {
    DEFAULT_FROM_TIME_STRING,
    DEFAULT_TO_TIME_STRING,
    FALLBACK_FROM_STRING,
    FALLBACK_TO_STRING,
    getEndOfDay,
    RangeSettingsActionType,
} from "../../hooks/filters/UseDateRange/UseDateRange";
import { FilterManager } from "../../hooks/filters/UseFilterManager/UseFilterManager";
import { PageSettingsActionType } from "../../hooks/filters/UsePages/UsePages";
import { FeatureName } from "../../utils/FeatureName";
import { appInsights } from "../../utils/TelemetryService/TelemetryService";

export enum StyleClass {
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
    setSearchTerm: React.Dispatch<React.SetStateAction<string>>;
    searchTerm: string;
    onFilterClick?: ({ from, to }: { from: string; to: string }) => void;
    receivers: { value: string; label: string }[];
    setService: Dispatch<SetStateAction<string>>;
    showDateHints?: boolean;
    startDateLabel: string;
    resultLength?: number;
    deliveriesHistoryDataUpdatedAt?: number;
}

// using a regex to check for format because of different browsers' implementations of Date
// e.g.:
//   new Date('11') in Chrome --> Date representation of 11/01/2001
//   new Date('11') in Firefox --> Invalid Date
const DATE_RE = /^[0-9]{1,2}\/[0-9]{1,2}\/[0-9]{2,4}$/;

export function isValidDateString(dateStr?: string) {
    // need to check for value format (mm/dd/yyyy) and date validity (no 99/99/9999)
    return DATE_RE.test(dateStr ?? "") && !Number.isNaN(Date.parse(dateStr ?? ""));
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
    setSearchTerm,
    searchTerm,
    onFilterClick,
    receivers,
    setService,
    showDateHints,
    startDateLabel,
    resultLength,
    deliveriesHistoryDataUpdatedAt,
}: TableFilterProps) {
    // Don't autofill the 2000-3000 date range.
    const fromStr =
        filterManager.rangeSettings.from === FALLBACK_FROM_STRING
            ? undefined
            : new Date(filterManager.rangeSettings.from);
    const toStr =
        filterManager.rangeSettings.to === FALLBACK_TO_STRING ? undefined : new Date(filterManager.rangeSettings.to);

    // store ISO strings to pass to FilterManager when user clicks 'Filter'
    // TODO: Remove FilterManager and CursorManager
    const [rangeFrom, setRangeFrom] = useState<Date | undefined>(fromStr);
    const [rangeTo, setRangeTo] = useState<Date | undefined>(toStr);
    const formRef = useRef<HTMLFormElement>(null);
    const [startTime, setStartTime] = useState(DEFAULT_FROM_TIME_STRING);
    const [endTime, setEndTime] = useState(DEFAULT_TO_TIME_STRING);
    const [currentServiceSelect, setCurrentServiceSelect] = useState<string>("");
    const [filterStatus, setFilterStatus] = useState<TableFilterData>({
        resultLength: resultLength,
        activeFilters: [currentServiceSelect],
    });
    const [filterReset, setFilterReset] = useState(0);
    const [searchReset, setSearchReset] = useState(0);
    const updateRange = useCallback(
        (from: string, to: string) => {
            filterManager.updateRange({
                type: RangeSettingsActionType.RESET,
                payload: { from, to },
            });

            // Reset results to first page upon applying filters
            filterManager.updatePage({
                type: PageSettingsActionType.SET_PAGE,
                payload: { page: 1 },
            });

            cursorManager?.update({
                type: CursorActionType.RESET,
                payload: filterManager.sortSettings.order === "DESC" ? to : from,
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
            startTime === DEFAULT_FROM_TIME_STRING &&
            endTime === DEFAULT_TO_TIME_STRING
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

        const rangeFromWithTime = new Date(rangeFrom.setHours(startHours, startMinutes, 0, 0)).toISOString();
        const rangeToWithTime = new Date(rangeTo.setHours(endHours, endMinutes, 0, 0)).toISOString();

        const isFilterDisabled = Boolean(new Date(rangeFromWithTime) > new Date(rangeToWithTime));

        return { isFilterDisabled, rangeFromWithTime, rangeToWithTime };
    }, [currentServiceSelect, endTime, rangeFrom, rangeTo, startTime]);
    const [domContentLoaded, setDomContentLoaded] = useState(false);

    // Using the native JS API to determine when the DOM is fully loaded
    useEffect(() => {
        const handleDOMContentLoaded = () => {
            setDomContentLoaded(true);
        };

        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", handleDOMContentLoaded);
        } else {
            // Document is already ready
            handleDOMContentLoaded();
        }

        return () => {
            document.removeEventListener("DOMContentLoaded", handleDOMContentLoaded);
        };
    }, []);

    // These variable and the logic below are for a specific use case:
    // We do NOT want to show the time range portion of the FilterStatus
    // when a user didn't manipulate both the Start time and End time.
    // However, to our controlled components, our input is always applied,
    // so we have to access the inputs via an uncontrolled method to see if
    // a user manipulated them or now.
    const startTimeElm = formRef?.current?.querySelector("#start-time") as HTMLInputElement | null;
    const endTimeElm = formRef?.current?.querySelector("#end-time") as HTMLInputElement | null;
    const showDefaultStatus = useMemo(() => {
        return (
            !currentServiceSelect && !rangeFrom && !rangeTo && !startTimeElm?.value && !endTimeElm?.value && !searchTerm
        );
    }, [currentServiceSelect, endTimeElm?.value, rangeFrom, rangeTo, searchTerm, startTimeElm?.value]);

    useEffect(() => {
        // This piece of code outputs into activeFilters a human readable
        // filter array for us to display on the FE, with protections against
        // undefined
        // Example Output: "elr, 03/04/24-03/07/24, 12:02am-04:25pm"
        // If user is using search, override filter display and just show searchTerm
        if (domContentLoaded) {
            setFilterStatus({
                resultLength: resultLength,
                activeFilters: [
                    ...(searchTerm.length
                        ? [searchTerm]
                        : [
                              currentServiceSelect,
                              [
                                  ...(rangeFrom && isValid(rangeFrom) ? [format(rangeFrom, "MM/dd/yyyy")] : []),
                                  ...(rangeTo && isValid(rangeTo) ? [format(rangeTo, "MM/dd/yyyy")] : []),
                              ].join("–"),
                              [
                                  ...(startTime ? [format(parse(startTime, "HH:mm", new Date()), "h:mm a")] : []),
                                  ...(endTime ? [format(parse(endTime, "HH:mm", new Date()), "h:mm a")] : []),
                              ]
                                  .join("–")
                                  .toLowerCase()
                                  .split(" ")
                                  .join(""),
                          ]),
                ],
            });
        }

        // We ONLY want to update the TableFilterStatus when loading is complete
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [deliveriesHistoryDataUpdatedAt, resultLength, domContentLoaded]);

    /* Pushes local state to context and resets cursor to page 1 */
    const applyToFilterManager = useCallback(
        (from: string, to: string) => {
            updateRange(from, to);

            // call onFilterClick with the specified range
            if (onFilterClick) onFilterClick({ from, to });
        },
        [onFilterClick, updateRange],
    );

    const resetFilterFields = useCallback(
        (e: FormEvent) => {
            e.preventDefault();
            setFilterReset(filterReset + 1);
            setRangeFrom(undefined);
            setRangeTo(undefined);
            setStartTime(DEFAULT_FROM_TIME_STRING);
            setEndTime(DEFAULT_TO_TIME_STRING);
            setCurrentServiceSelect("");
            setService("");
            filterManager.resetAll();
        },
        [filterManager, filterReset, setService],
    );

    /* Clears manager and local state values */
    const resetHandler = useCallback(
        (e: FormEvent) => {
            e.preventDefault();
            setSearchTerm("");
            resetFilterFields(e);
            setSearchReset(searchReset + 1);

            appInsights?.trackEvent({
                name: `${FeatureName.DAILY_DATA} | Reset`,
            });
        },
        [resetFilterFields, searchReset, setSearchTerm],
    );

    const submitHandler = useCallback(
        (e: FormEvent) => {
            e.preventDefault();
            setSearchTerm("");
            setSearchReset(searchReset + 1);

            setService?.(currentServiceSelect);
            if (filterDetails.rangeFromWithTime && filterDetails.rangeToWithTime) {
                applyToFilterManager(filterDetails.rangeFromWithTime, filterDetails.rangeToWithTime);
            }

            appInsights?.trackEvent({
                name: `${FeatureName.DAILY_DATA} | Apply`,
            });
        },
        [
            applyToFilterManager,
            currentServiceSelect,
            filterDetails.rangeFromWithTime,
            filterDetails.rangeToWithTime,
            searchReset,
            setSearchTerm,
            setService,
        ],
    );

    return (
        <div className={styles.TableFilters}>
            <TableFilterSearch
                resetHandler={resetHandler}
                searchReset={searchReset}
                setSearchTerm={setSearchTerm}
                resetFilterFields={resetFilterFields}
            />

            <section data-testid="filter-container" className="filter-container flex-column">
                <p className="text-bold margin-top-0 grid-col-12">
                    View data from a specific receiver or date and time range
                </p>
                <form
                    ref={formRef}
                    onSubmit={submitHandler}
                    onReset={resetHandler}
                    key={filterReset}
                    autoComplete="off"
                    data-testid="filter-form"
                    className="width-full"
                >
                    <div className="grid-row">
                        <div className="grid-col-3 filter-column__one">
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
                                <option disabled key={""} value={""}>
                                    {""}
                                </option>
                                {receivers?.map((receiver) => (
                                    <option key={receiver.value} value={receiver.value}>
                                        {receiver.value}
                                    </option>
                                ))}
                            </Select>
                        </div>
                        <div className="grid-col-7 filter-column__two">
                            <DateRangePicker
                                className={StyleClass.DATE_CONTAINER}
                                startDateLabel={startDateLabel}
                                startDateHint={showDateHints ? "mm/dd/yyyy" : ""}
                                startDatePickerProps={{
                                    id: "start-date",
                                    name: "start-date-picker",
                                    defaultValue: rangeFrom?.toISOString(),
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
                                    defaultValue: rangeTo?.toISOString(),
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
                                                setStartTime(DEFAULT_FROM_TIME_STRING);
                                            }
                                        }}
                                    />
                                    <p className="usa-hint usa-hint__default">Default: 12:00am</p>
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
                                                setEndTime(DEFAULT_TO_TIME_STRING);
                                            }
                                        }}
                                    />
                                    <p className="usa-hint usa-hint__default">Default: 11:59pm</p>
                                </div>
                            </div>
                        </div>
                        <div className="grid-col-2 filter-column__three">
                            <div className="button-container">
                                <div>
                                    <Button
                                        className="margin-right-205"
                                        disabled={filterDetails.isFilterDisabled}
                                        type={"submit"}
                                    >
                                        Apply
                                    </Button>
                                    <Button type={"reset"} name="clear-button" unstyled>
                                        Reset
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </div>
                </form>
            </section>
            <TableFilterStatus filterStatus={filterStatus} showDefaultStatus={showDefaultStatus} />
        </div>
    );
}

export default TableFilters;
