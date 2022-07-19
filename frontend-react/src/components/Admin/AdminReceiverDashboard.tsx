import { useResource } from "rest-hooks";
import React, { useState } from "react";
import {
    DateRangePicker,
    Grid,
    GridContainer,
    Label,
    TextInput,
} from "@trussworks/react-uswds";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import {
    AdmConnStatusResource,
    AdmConnStatusDataType,
} from "../../resources/AdmConnStatusResource";
import { StyleClass } from "../Table/TableFilters";

/**
 * UI driven by data "shape".
 *
 * Cron runs every 2 hours.
 * OrgID+RecvID is unique, there are ~103 right now.
 * Recv Name is NOT unique. But "OrgName RecvName" is unique
 *
 *
 * First attempt was to use nested arrays to group data for easier display
 * [outer group by org+recv][inner group by hour of day], but it got complicated FAST
 * Instead, we're going to borrow a concept from key-value databases.
 * https://en.wikipedia.org/wiki/Key%E2%80%93value_database
 *
 * Keys will be ORDERED PATH of value and therefore similar data will adjacent.
 *
 */

const SKIP_HOURS = 2; // hrs
const DAY_BACK_DEFAULT = 5 - 1; // 5 days (-1 because we add a day later for ranges)

/**
 * Declared outside of the function so React doesn't update constantly (also, time is fixed)
 * Usage: `yesterday()`
 * @param d {Date}
 * @return {string}
 */
const defaultStartDateIso = (d = new Date()) => {
    d.setHours(-24 * DAY_BACK_DEFAULT, 0, 0, 0); // DAY_BACK_DEFAULT days ago
    return d.toISOString();
};

const defaultEndDateIso = (d = new Date()) => {
    d.setHours(0, 0, 0, 0);
    return d.toISOString();
};

const roundIsoDateDown = (iso: string) => {
    const parts = iso.split("T");
    const endsInZ = iso[iso.length - 1] === "Z";
    return `${parts[0]}T00:00:00.000${endsInZ ? "Z" : ""}`;
};

const roundIsoDateUp = (iso: string) => {
    const d = new Date(iso);
    d.setDate(d.getDate() + 1); // add 1 day
    const isoNewD = d.toISOString();
    const parts = isoNewD.split("T");
    const endsInZ = iso[iso.length - 1] === "Z";
    return `${parts[0]}T00:00:00.000${endsInZ ? "Z" : ""}`;
};

// key/value pair
type DataDictionary = Record<string, AdmConnStatusDataType>;
// prop drilled down to bottom most components
// type dateClickHandler = (id: string) => void;

enum SuccessRate {
    UNDEFINED,
    ALL_SUCCESSFUL,
    ALL_FAILURE,
    MIXED_SUCCESS,
}

/** simple container for logic related to tracking if a run is all success or all failure or mixed.
 * Originally was a reducer, but that was overkill and the hook made using it harder. **/
class SuccessRateTracker {
    currentState: SuccessRate;
    countSuccess: number;
    countFailed: number;

    constructor() {
        this.currentState = SuccessRate.UNDEFINED;
        this.countSuccess = this.countFailed = 0;
    }

    updateState(newResult: boolean) {
        if (newResult) {
            this.countSuccess++;
            // true and we're in one of these states
            this.currentState = [
                SuccessRate.UNDEFINED,
                SuccessRate.ALL_SUCCESSFUL,
            ].includes(this.currentState)
                ? SuccessRate.ALL_SUCCESSFUL
                : SuccessRate.MIXED_SUCCESS;
        } else {
            this.countFailed++;
            // false and we're in one of these states
            this.currentState = [
                SuccessRate.UNDEFINED,
                SuccessRate.ALL_FAILURE,
            ].includes(this.currentState)
                ? SuccessRate.ALL_FAILURE
                : SuccessRate.MIXED_SUCCESS;
        }
        return this.currentState;
    }

    reset() {
        this.currentState = SuccessRate.UNDEFINED;
        this.countSuccess = 0;
        this.countFailed = 0;
    }
}

const SUCCESS_BK_CLASSNAME_MAP = {
    [SuccessRate.UNDEFINED]: "rs-admindash-bk-success-undefined",
    [SuccessRate.ALL_SUCCESSFUL]: "rs-admindash-bk-success-all",
    [SuccessRate.ALL_FAILURE]: "rs-admindash-bk-failure-all",
    [SuccessRate.MIXED_SUCCESS]: "rs-admindash-bk-success-mixed",
};

const SUCCESS_BORDER_CLASSNAME_MAP = {
    [SuccessRate.UNDEFINED]: "rs-admindash-border-success-undefined",
    [SuccessRate.ALL_SUCCESSFUL]: "rs-admindash-border-success-all",
    [SuccessRate.ALL_FAILURE]: "rs-admindash-border-failure-all",
    [SuccessRate.MIXED_SUCCESS]: "rs-admindash-border-success-mixed",
};

function dateAddHours(d: Date, h: number): Date {
    const result = new Date(d); // copy value
    result.setHours(result.getHours() + h);
    return result;
}

// mostly for readably
type DatePair = [Date, Date];

function dateIsInRange(d: Date, range: DatePair): boolean {
    return d >= range[0] && d < range[1];
}

/**
 *  simple iterator to make other code more readable.
 *  Usage:
 *    for (let eachTimeSlot in (new TimeSlots([dateStart, dateEnd], 2)) { }
 */
interface IterateTimeSlots {
    [Symbol.iterator]: () => Iterator<DatePair>;
}

class TimeSlots implements IterateTimeSlots {
    private current: Date;
    private readonly end: Date;
    private readonly skipHours: number;

    constructor(range: DatePair, skipHours: number = 2) {
        this.current = range[0];
        this.end = range[1];
        this.skipHours = skipHours;
    }

    *[Symbol.iterator]() {
        do {
            yield [
                this.current,
                dateAddHours(this.current, this.skipHours),
            ] as DatePair;
            this.current = dateAddHours(this.current, this.skipHours);
        } while (this.current < this.end);
    }
}

// function splitUtcDateStr(input: string): { dateStr: string; hourStr: string } {
//     // handy observation, the UTC date format makes getting the time section easier.
//     // "2022-07-14T19:33:04.948Z" => ["2022-07-14", "19", "33", "04.948Z"]
//     // We WANT leading zeros for hour so that they sort correctly, thus hour is a string.
//     const timeParts = input.split(/[T,:]/);
//     return { dateStr: timeParts[0], hourStr: timeParts[1] };
// }

/**
 * build the dictionary with a special path+key
 * @param dataIn
 */
function makeDictionary(dataIn: AdmConnStatusDataType[]): DataDictionary {
    // empty case
    if (Object.keys(dataIn).length === 0) {
        return {};
    }

    const data: DataDictionary = {};
    let needsSorting = false;

    let lastkey = "";
    for (let item of dataIn) {
        const key = `${item.organizationName}|${item.receiverName}\t${item.connectionCheckStartedAt}`;
        data[key] = item;
        if (lastkey > key) {
            needsSorting = true;
        }
        lastkey = key;
    }

    if (!needsSorting) {
        return data;
    }

    // temp doubles data size
    const emptyResult: DataDictionary = {}; // keeps ts compiler happy
    return Object.keys(data)
        .sort()
        .reduce((obj, key) => {
            obj[key] = data[key] || {};
            return obj;
        }, emptyResult);
}

function MainRender(props: { data: DataDictionary; datesRange: DatePair }) {
    const keys = Object.keys(props.data);
    if (keys.length === 0) {
        return <div>No Data</div>;
    }

    // todo: logic to handle empty days on either end vs (user selected)
    // const [firstDate, lastDate] = getFirstLastDates(props.data);

    const startDay = props.datesRange[0];
    const endDay = props.datesRange[1];
    // the range is midnight to midnight, so the endDay needs to be +1 to be correct
    endDay.setDate(endDay.getDate() + 1);

    // we use double cursors (move through time and through entries)
    let keyOffset = 0;
    // readability
    const rowElements: JSX.Element[] = [];
    let dayElements: JSX.Element[] = [];
    let sliceElements: JSX.Element[] = [];

    // loop over all receivers (each is its own row)
    while (keyOffset < keys.length) {
        const successForRow = new SuccessRateTracker();
        const keyOffsetStartRow = keyOffset; // used during render
        let currentKey = keys[keyOffset];
        let currentEntry = props.data[currentKey];
        let currentDate = new Date(currentEntry.connectionCheckCompletedAt);
        let currentReceiver = `${currentEntry.organizationName}|${currentEntry.receiverName}`;
        let rowReceiver = currentReceiver; // used to know when we've run out of row data

        // loop over all days
        const daySlots = new TimeSlots([startDay, endDay], 24);
        for (let [daySlotStart, daySlotEnd] of daySlots) {
            const successForDay = new SuccessRateTracker();
            const timeSlots = new TimeSlots(
                [daySlotStart, daySlotEnd],
                SKIP_HOURS
            );
            for (let [timeSlotStart, timeSlotEnd] of timeSlots) {
                const successForSlice = new SuccessRateTracker();
                const hoverInfo: AdmConnStatusDataType[] = []; // used for hover - feels hacky

                // loop over keys that are in this range. Build aggregates
                const keysMatched = [];
                while (
                    keyOffset < keys.length &&
                    dateIsInRange(currentDate, [timeSlotStart, timeSlotEnd]) &&
                    rowReceiver === currentReceiver
                ) {
                    // because we're moving two cursors through data (date slots and data results),
                    // we use the data results from the prior iteration when we moved that cursor forward.
                    keysMatched.push(currentKey);
                    const wasSuccessful =
                        currentEntry.connectionCheckSuccessful;
                    successForSlice.updateState(wasSuccessful);
                    successForDay.updateState(wasSuccessful);
                    successForRow.updateState(wasSuccessful);
                    hoverInfo.push(currentEntry);

                    // next entry
                    keyOffset++;
                    if (keyOffset >= keys.length) {
                        break;
                    }
                    currentKey = keys[keyOffset];
                    currentEntry = props.data[currentKey];
                    currentDate = new Date(
                        currentEntry.connectionCheckCompletedAt
                    );
                    currentReceiver = `${currentEntry.organizationName}|${currentEntry.receiverName}`;
                }

                {
                    const sliceClassName =
                        SUCCESS_BK_CLASSNAME_MAP[successForSlice.currentState];

                    // we can use the currentkey in the data dict as a unique key for this cell
                    // todo: include starttime and hover information. (use hoverInfo)
                    console.log(`use hoverInfo`, JSON.stringify(hoverInfo));
                    sliceElements.push(
                        <Grid
                            row
                            key={`slice:${currentReceiver}|${timeSlotStart}`}
                            className={`rs-admindash-per-slot ${sliceClassName}`}
                            title={`${currentReceiver}\n${timeSlotStart}`}
                        >
                            {" "}
                        </Grid>
                    );
                }
            }
            // render day using successForDay and current day info
            {
                const dateStr = Intl.DateTimeFormat("en-US", {
                    weekday: "short",
                    year: "numeric",
                    month: "numeric",
                    day: "numeric",
                }).format(daySlotStart);

                dayElements.push(
                    <GridContainer className={"rs-admindash-week"}>
                        <Grid row className={"rs-admindash-perday-title"}>
                            {dateStr}
                        </Grid>
                        <Grid
                            gap={1}
                            row
                            className={"rs-admindash-12perday-section"}
                        >
                            {sliceElements}
                        </Grid>
                    </GridContainer>
                );
                sliceElements = [];
            }
        } // for dayslots

        // render row using successForRow and current receiver name
        {
            // we saved the start of this block of data, grab the information from there.
            const key = keys[keyOffsetStartRow];
            const orgName = props.data[key].organizationName;
            const recvrName = props.data[key].receiverName;
            const successRate = Math.round(
                (100 * successForRow.countSuccess) /
                    (successForRow.countSuccess + successForRow.countFailed)
            );

            const titleClassName =
                SUCCESS_BORDER_CLASSNAME_MAP[successForRow.currentState];

            // todo: improve layout of row header
            rowElements.push(
                <Grid
                    row
                    key={`row|${key}`}
                    className={"rs-admindash-receiver-row"}
                >
                    <Grid
                        className={`rs-admindash-receiver-row-title ${titleClassName}`}
                        title={`${orgName}\n${recvrName}`}
                    >
                        {orgName}
                        <br />
                        {recvrName}
                        <br />
                        {successRate}%
                    </Grid>
                    <Grid className={"rs-admindash-days-row"}>
                        <ScrollSyncPane>
                            {/*<div className={"rs-admindash-horizonal-scroll"}>*/}
                            <GridContainer
                                className={"rs-admindash-container-days"}
                            >
                                <Grid
                                    row
                                    className={"rs-admindash-days-row"}
                                    key={`week|${currentReceiver}`}
                                >
                                    <Grid
                                        row
                                        className={"rs-admindash-progress-row"}
                                        key={`days|${currentReceiver}`}
                                    >
                                        {dayElements}
                                    </Grid>
                                </Grid>
                            </GridContainer>
                            {/*</div>*/}
                        </ScrollSyncPane>
                    </Grid>
                </Grid>
            );
            dayElements = [];
        }
        keyOffset++;
    } // while
    return (
        <ScrollSync horizontal={true} vertical={false}>
            <GridContainer
                className={"rs-admindash-main-container"}
                key={"AdminReceiverDashboard"}
            >
                {rowElements}
            </GridContainer>
        </ScrollSync>
    );
}

// function getFirstLastEntries(data: DataDictionary) {
//     const keys = Object.keys(data);
//     const firstkey = keys[0];
//     const lastkey = keys[keys.length];
//     return {
//         first: data[firstkey],
//         last: data[lastkey],
//     };
// }

// function getFirstLastDates(data: DataDictionary): DatePair {
//     const { first, last } = getFirstLastEntries(data);
//     return [
//         new Date(first.connectionCheckStartedAt),
//         new Date(last.connectionCheckStartedAt),
//     ];
// }

export function AdminReceiverDashboard() {
    const [startDate, setStartDate] = useState<string>(defaultStartDateIso());
    const [endDate, setEndDate] = useState<string | undefined>(); // may be null because it's optional
    const results = useResource(AdmConnStatusResource.list(), {
        startDate,
        endDate,
    });
    // this is the text input box filter
    const [filterReceivers, setFilterReceivers] = useState("");
    const [filterErrorResults, setFilterErrorResults] = useState("");

    const data = makeDictionary(
        results.filter(
            (eachRow) =>
                eachRow.filterOnName(filterReceivers) &&
                eachRow.filterOnCheckResultStr(filterErrorResults)
        )
    );

    return (
        <>
            <section className="grid-container margin-1">
                <h4>Receiver dashboards</h4>
                <form autoComplete="off" className="grid-row margin-0">
                    <div className="flex-auto">
                        <DateRangePicker
                            className={`${StyleClass.DATE_CONTAINER} margin-1`}
                            startDateLabel="From (Start Range):"
                            startDatePickerProps={{
                                id: "start-date",
                                name: "start-date-picker",
                                defaultValue: defaultStartDateIso(),
                                onChange: (s) =>
                                    setStartDate(
                                        s
                                            ? new Date(s).toISOString()
                                            : defaultStartDateIso()
                                    ),
                            }}
                            endDateLabel="Until (End Range):"
                            endDatePickerProps={{
                                id: "end-date",
                                name: "end-date-picker",
                                defaultValue: defaultEndDateIso(),
                                onChange: (s) =>
                                    setEndDate(
                                        s
                                            ? new Date(s).toISOString()
                                            : undefined
                                    ),
                            }}
                        />
                    </div>
                    <div className="flex-fill margin-1">
                        <Label
                            className="font-sans-xs usa-label"
                            htmlFor="input_filter_receivers"
                        >
                            Filter receiver rows:
                        </Label>
                        <TextInput
                            id="input_filter_receivers"
                            name="input_filter_receivers"
                            type="text"
                            autoComplete="off"
                            aria-autocomplete="none"
                            autoFocus
                            onBlur={(evt) =>
                                setFilterReceivers(evt.target.value)
                            }
                        />
                    </div>

                    <div className="flex-fill margin-1">
                        <Label
                            className="font-sans-xs usa-label"
                            htmlFor="input_filter_errors"
                        >
                            Filter on network results:
                        </Label>
                        <TextInput
                            id="input_filter_errors"
                            name="input_filter_errors"
                            type="text"
                            autoComplete="off"
                            aria-autocomplete="none"
                            autoFocus
                            onBlur={(evt) =>
                                setFilterErrorResults(evt.target.value)
                            }
                        />
                    </div>
                </form>
                {MainRender({
                    data,
                    datesRange: [
                        new Date(startDate),
                        endDate ? new Date(endDate) : new Date(),
                    ],
                })}
            </section>
        </>
    );
}

export const _exportForTesting = {
    TimeSlots,
    roundIsoDateDown,
    roundIsoDateUp,
    SuccessRateTracker,
    SuccessRate,
};
