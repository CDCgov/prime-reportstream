import { useResource } from "rest-hooks";
import React, { useCallback, useRef, useState } from "react";
import {
    DateRangePicker,
    Dropdown,
    Grid,
    GridContainer,
    Label,
    Modal,
    ModalRef,
    TextInput,
    Tooltip,
} from "@trussworks/react-uswds";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import {
    AdmConnStatusResource,
    AdmConnStatusDataType,
} from "../../resources/AdmConnStatusResource";
import { StyleClass } from "../Table/TableFilters";
import { formatDate } from "../../utils/misc";

/**
 *
 * Cron runs every 2 hours (in production)
 * OrgID+RecvID is unique, there are ~103 right now.
 *
 * First attempt was to use nested arrays to group data for easier display
 * [outer group by org+recv][inner group by hour of day], but it got complicated FAST
 * Instead, we're going to borrow a concept from key-value databases.
 * https://en.wikipedia.org/wiki/Key%E2%80%93value_database
 *
 * Keys will be ORDERED PATH of value and therefore similar data will be adjacent.
 *
 * There are 3 nested loops for layout of the data
 *    foreach per-receiver:
 *       foreach per-day loop:
 *          foreach per-timeblock loop: (also called "slice" in this code)
 *
 * And since the key-value dictionary is sorted this way, then there's a single
 * cursor moving through it. (NOTE: there might be missing days or slices in the dictionary.
 * This can happen when a new receiver is onboarded in the middle of the data.
 * Or if the CRON job doesn't run because of deploy or outage leaving slice holes)
 *
 *
 * Layout can be confusing, so hopefully this helps.
 * Also, the scss follows this SAME hierarchy layout
 *
 * "Dashboard" component
 *
 *                  .rs-admindash-component
 * ├──────────────────────────────────────────────────────┤
 * ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓──┬
 * ┃ ┌──────────────────────────────────────────────────┐ ┃  │
 * ┃ │                                                  │ ┃  │
 * ┃ │                                                  │ ┃  │ `.perreceiver-row`
 * ┃ │                                                  │ ┃  │
 * ┃ └──────────────────────────────────────────────────┘ ┃  │
 * ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫──┴
 * ┃ ┌──────────────────────────────────────────────────┐ ┃
 * ┃ │                                                  │ ┃
 * ┃ │                                                  │ ┃
 * ┃ │                                                  │ ┃
 * ┃ └──────────────────────────────────────────────────┘ ┃
 * ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
 * ┃ ┌──────────────────────────────────────────────────┐ ┃
 * ┃ │                                                  │ ┃
 * ┃ │    ▲                                             │ ┃
 * ┃ │    │                                             │ ┃
 * ┃ └───━│─────────────────────────────────────────────┘ ┃
 * ┗━━━━━━│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 *        └─── PerReceiverComponent
 *
 *
 * "PerReceiver" component
 *  outer grid is one row
 *  inner grid is two columns
 *
 *        ┌ .title-text                    ┌ PerDay components from above
 *        │                                │
 * ┏━━━━━━│━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
 * ┃  ┌───┼───┐  ┃   ┌──────┐ ┌──────┐ ┌───┼──┐ ┌──────┐ ┌──────┐ ┌──────┐    ┃
 * ┃  │   │   │  ┃   │      │ │      │ │   │  │ │      │ │      │ │      │    ┃
 * ┃  │   ▼   │  ┃   │      │ │      │ │   ▼  │ │      │ │      │ │      │    ┃
 * ┃  │       │  ┃   │      │ │      │ │      │ │      │ │      │ │      │    ┃
 * ┃  └───────┘  ┃   └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘    ┃
 * ┗━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 * ││           │ │                                                         │ │
 * │├───────────┤ ├─────────────────────────────────────────────────────────┤ │
 * │     ▲                          .week-column                              │
 * │     └─ .title-column                                                     │
 * ├──────────────────────────────────────────────────────────────────────────┤
 *                               .perreceiver-component
 *
 *
 *
 * "PerDay" component
 *
 *    Grid is two rows.
 *    [inner grid] is a bunch of very fall columns
 *  ┏━━━━━━━━━━━━━━━━━┓ ─────┬
 *  ┃  .perday-title  ┃      │ .title-row (Row 1)
 *  ┣━━┯━━┯━━┯━━┯━━┯━━┫ ─────┼
 *  ┃  │  │  │  │  │  ┃      │
 *  ┃  │  │  │  │  │  ┃      │
 *  ┃  │  │  │  │  │  ┃      │
 *  ┃  [inner grid]   ┃      │ .slices-row^ (Row 2)
 *  ┃  │  │  │  │  │  ┃      │
 *  ┃  │  │  │  │  │  ┃      │
 *  ┃  │  │▲ │  │  │  ┃      │
 *  ┗━━┷━━┷│━┷━━┷━━┷━━┛ ─────┴
 *  │      └──────────┼─────.slice^ (each)
 *  ├─────────────────┤
 *    .perday-component
 *
 *
 * ^NOTES: - perday-slices-row needs to know the number of total slices.
 *           Right now there are layouts for 12/day (every 2hrs) and 4/day (every 6hrs)
 *         - perday-perslice-column has 4 color states as well
 *
 */

const SKIP_HOURS = 2; // hrs
const DAY_BACK_DEFAULT = 7 - 1; // N days (-1 because we add a day later for ranges)

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

/**
 * Result string is like "12h 34m 05.678s"
 * No duration returns ""
 * @param dateNewer Date
 * @param dateOlder Date
 */
const durationFormatShort = (dateNewer: Date, dateOlder: Date): string => {
    const msDiff = dateNewer.getTime() - dateOlder.getTime();
    const hrs = Math.floor(msDiff / (60 * 60 * 1000)).toString();
    const mins = Math.floor((msDiff / (60 * 1000)) % 60).toString();
    // 0.1200001 -> '0.12`
    const secs = parseFloat(((msDiff / 1000) % 60).toFixed(3)).toString();

    const parts = [];
    if (hrs !== "0") {
        parts.push(`${hrs}h`);
    }
    if (parts.length || mins !== "0") {
        const mins_pad = mins.length < 2 ? "0" + mins : mins;
        parts.push(`${mins_pad}m`);
    }
    if (parts.length || secs !== "0") {
        const secs_pad = secs.indexOf(".") < 2 ? "0" + secs : secs;
        parts.push(`${secs_pad}s`);
    }
    return parts.join(" ");
};

/*
 * format: "Mon, 7/25/2022"
 * WARNING: Intl.DateTimeFormat() can be slow if called in a loop!
 * Rewrote to just use Date to save cpu
 * */
const dateShortFormat = (d: Date) => {
    const dayOfWeek =
        ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][d.getDay()] || "";
    return (
        `${dayOfWeek}, ` +
        `${d.getMonth() + 1}/${d.getDate()}/${d.getFullYear()}`
    );
};

// key/value pair
type DataDictionary = Record<string, AdmConnStatusDataType>;

enum SuccessRate {
    UNDEFINED = "UNDEFINED",
    ALL_SUCCESSFUL = "ALL_SUCCESSFUL",
    ALL_FAILURE = "ALL_FAILURE",
    MIXED_SUCCESS = "MIXED_SUCCESS",
}

/** simple container for logic related to tracking if a run is all success or all failure or mixed.
 * Originally was a reducer, but the hook limitations made using it harder to use. **/
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

const SUCCESS_RATE_CLASSNAME_MAP = {
    [SuccessRate.UNDEFINED]: "success-undefined",
    [SuccessRate.ALL_SUCCESSFUL]: "success-all",
    [SuccessRate.ALL_FAILURE]: "failure-all",
    [SuccessRate.MIXED_SUCCESS]: "success-mixed",
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
            const endTime = dateAddHours(this.current, this.skipHours);
            yield [this.current, endTime] as DatePair;
            this.current = endTime;
        } while (this.current < this.end);
    }
}

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

function MainRender(props: {
    data: DataDictionary;
    datesRange: DatePair;
    filterRowStatus: SuccessRate;
    onDetailsClick: (subData: AdmConnStatusDataType[]) => void;
}) {
    const onClick = useCallback(
        (dataItem: AdmConnStatusDataType) => {
            // in theory, there might be multiple events for the block, but we're only handling one for now.
            props.onDetailsClick([dataItem]);
        },
        [props]
    );

    const keys = Object.keys(props.data);
    if (keys.length === 0) {
        return <div>No Data</div>;
    }

    const startDay = props.datesRange[0];
    const endDay = props.datesRange[1];
    // the range is midnight to midnight, so the endDay needs to be +1 to be correct
    endDay.setDate(endDay.getDate() + 1);

    // we use double cursors (move through time and through entries)
    let keyOffset = 0;
    // readability
    const perReceiverElements: JSX.Element[] = [];
    let perDayElements: JSX.Element[] = [];
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

                // loop over keys that are in this range. Build aggregates
                while (
                    keyOffset < keys.length &&
                    dateIsInRange(currentDate, [timeSlotStart, timeSlotEnd]) &&
                    rowReceiver === currentReceiver
                ) {
                    // because we're moving two cursors through data (date slots and data results),
                    // we use the data results from the prior iteration when we moved that cursor forward.
                    const wasSuccessful =
                        currentEntry.connectionCheckSuccessful;
                    successForSlice.updateState(wasSuccessful);
                    successForDay.updateState(wasSuccessful);
                    successForRow.updateState(wasSuccessful);

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

                // <editor-fold desc="Time slices for a given day render">
                const sliceClassName =
                    SUCCESS_RATE_CLASSNAME_MAP[successForSlice.currentState];

                // we can use the currentkey in the data dict as a unique key for this cell
                sliceElements.push(
                    <Grid
                        row
                        key={`slice:${currentReceiver}|${timeSlotStart}`}
                        className={`slice ${sliceClassName}`}
                        data-keyoffset={keyOffset - 1}
                        onClick={
                            successForSlice.currentState ===
                            SuccessRate.UNDEFINED
                                ? undefined // do not even install a click handler noop
                                : (evt) => {
                                      // get saved offset from "data-keyoffset" attribute on this element
                                      const dataKeyOffset = parseInt(
                                          evt.currentTarget?.dataset[
                                              "keyoffset"
                                          ] || "-1"
                                      );
                                      // sanity check it's within range (should never happen)
                                      if (
                                          dataKeyOffset >= 0 &&
                                          dataKeyOffset < keys.length
                                      ) {
                                          const key = keys[dataKeyOffset];
                                          onClick(props.data[key]);
                                      }
                                  }
                        }
                    >
                        {" "}
                    </Grid>
                );
                // </editor-fold>
            }

            // <editor-fold desc="Per-Day render">
            const dateStr = dateShortFormat(daySlotStart);

            perDayElements.push(
                <GridContainer
                    key={`perday-${dateStr}`}
                    className={"perday-component"}
                >
                    <Grid row className={"title-row"}>
                        {dateStr}
                    </Grid>
                    <Grid gap={1} row className={"slices-row slices-row-12"}>
                        {sliceElements}
                    </Grid>
                </GridContainer>
            );
            sliceElements = [];
            // </editor-fold>
        } // for dayslots

        // <editor-fold desc="Per-Receiver render">
        const showRow =
            props.filterRowStatus !== SuccessRate.UNDEFINED
                ? props.filterRowStatus === successForRow.currentState
                : true;

        if (showRow) {
            // we saved the start of this block of data, grab the information from there.
            const key = keys[keyOffsetStartRow];
            const orgName = props.data[key].organizationName;
            const recvrName = props.data[key].receiverName;
            const successRate = Math.round(
                (100 * successForRow.countSuccess) /
                    (successForRow.countSuccess + successForRow.countFailed)
            );
            const titleClassName =
                SUCCESS_RATE_CLASSNAME_MAP[successForRow.currentState];

            perReceiverElements.push(
                <Grid
                    row
                    key={`perreceiver-row-${keyOffsetStartRow}`}
                    className={"perreceiver-row"}
                >
                    <Grid className={`title-column ${titleClassName}`}>
                        <div className={"title-text"}>
                            {orgName}
                            <br />
                            {recvrName}
                            <br />
                            {successRate}%
                        </div>
                    </Grid>
                    <ScrollSyncPane enabled>
                        <Grid row className={"horizontal-scroll"}>
                            <Grid row className={"week-column"}>
                                {perDayElements}
                            </Grid>
                        </Grid>
                    </ScrollSyncPane>
                </Grid>
            );
        }
        perDayElements = [];
        // </editor-fold>
        keyOffset++;
    } // while
    return (
        //
        <ScrollSync horizontal enabled>
            <GridContainer className={"rs-admindash-component"}>
                {perReceiverElements}
            </GridContainer>
        </ScrollSync>
    );
}

function ModalInfoRender(props: { subData: AdmConnStatusDataType[] }) {
    if (!props?.subData.length) {
        return <>No Data Found</>;
    }
    // note: if we ever have the timeslots > cron job so there are multiple
    // results per slot, then this needs to be expended to show more.
    const dataItem = props.subData[0];

    const duration = () => {
        return durationFormatShort(
            new Date(dataItem.connectionCheckCompletedAt),
            new Date(dataItem.connectionCheckStartedAt)
        );
    };

    return (
        <GridContainer className={"rs-admindash-modal"}>
            <Grid className={"modal-info-title"}>
                Results for connection verification check
            </Grid>
            <Grid row className={"modal-info-row"}>
                <Grid className={"modal-info-label"}>Org:</Grid>
                <Grid className={"modal-info-value"}>
                    {dataItem.organizationName} (id: {dataItem.organizationId})
                </Grid>
            </Grid>

            <Grid row className={"modal-info-row"}>
                <Grid className={"modal-info-label "}>Receiver:</Grid>
                <Grid className={"modal-info-value"}>
                    {dataItem.receiverName} (id: {dataItem.receiverId})
                </Grid>
            </Grid>

            <Grid row className={"modal-info-row"}>
                <Grid className={"modal-info-label"}>Result:</Grid>
                <Grid
                    className={`modal-info-value ${
                        dataItem.connectionCheckSuccessful
                            ? "success-all"
                            : "failure-all"
                    }`}
                >
                    {dataItem.connectionCheckSuccessful ? "success" : "failed"}
                </Grid>
            </Grid>

            <Grid row className={"modal-info-row"}>
                <Grid className={"modal-info-label"}>Started At:</Grid>
                <Grid className={"modal-info-value"}>
                    {formatDate(dataItem.connectionCheckStartedAt)}
                    <br />
                    {dataItem.connectionCheckStartedAt}
                </Grid>
            </Grid>

            <Grid row className={"modal-info-row"}>
                <Grid className={"modal-info-label"}>Time to complete:</Grid>
                <Grid className={"modal-info-value"}>
                    {duration()}
                    <br />
                </Grid>
            </Grid>

            <Grid row className={"modal-info-row"}>
                <Grid className={"modal-info-label"}>Result message:</Grid>
                <Grid className={"modal-info-value"}>
                    {dataItem.connectionCheckResult}
                </Grid>
            </Grid>
        </GridContainer>
    );
}

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
    const [filterRowSuccessState, setFilterRowSuccessState] =
        useState<SuccessRate>(SuccessRate.UNDEFINED);

    // used to show hide the modal
    const modalShowInfoRef = useRef<ModalRef>(null);
    const [currentDataForModal, setCurrentDataForModal] = useState<
        AdmConnStatusDataType[]
    >([]);

    const data = makeDictionary(
        results.filter(
            (eachRow) =>
                eachRow.filterOnName(filterReceivers) &&
                eachRow.filterOnCheckResultStr(filterErrorResults)
        )
    );

    const showDetailsModal = (subData: AdmConnStatusDataType[]) => {
        if (subData.length) {
            setCurrentDataForModal(subData);
            modalShowInfoRef?.current?.toggleModal(undefined, true);
        }
    };

    return (
        <section className="grid-container">
            <h4>Receiver Status Dashboard</h4>
            <section>
                CRON job results that check if receivers are working.
                <br />
                Each slot is a 2hr time slice. Colored slots had a check run.
                Clicking on a slot shows details.
            </section>
            <form autoComplete="off" className="grid-row margin-0">
                <div className="flex-auto margin-1">
                    <DateRangePicker
                        className={`${StyleClass.DATE_CONTAINER} margin-0`}
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
                                    s ? new Date(s).toISOString() : undefined
                                ),
                        }}
                    />
                </div>
                <div className="flex-fill margin-1">
                    <Label
                        className="font-sans-xs usa-label text-no-wrap"
                        htmlFor="input_filter_receivers"
                    >
                        Receiver Name:
                    </Label>
                    <Tooltip
                        className="fixed-tooltip"
                        label="Filter rows on just the first column's Organization name and Receiver setting name."
                    >
                        <TextInput
                            id="input_filter_receivers"
                            name="input_filter_receivers"
                            type="text"
                            autoComplete="off"
                            aria-autocomplete="none"
                            autoFocus
                            onChange={(evt) =>
                                setFilterReceivers(evt.target.value)
                            }
                        />
                    </Tooltip>
                </div>

                <div className="flex-fill margin-1">
                    <Label
                        className="font-sans-xs usa-label text-no-wrap"
                        htmlFor="input_filter_errors"
                    >
                        Results Message:
                    </Label>
                    <Tooltip
                        className="fixed-tooltip"
                        label="Filter rows on the Result Message details. This value is found in the details."
                    >
                        <TextInput
                            id="input_filter_errors"
                            name="input_filter_errors"
                            type="text"
                            autoComplete="off"
                            aria-autocomplete="none"
                            autoFocus
                            onChange={(evt) =>
                                setFilterErrorResults(evt.target.value)
                            }
                        />
                    </Tooltip>
                </div>

                <div className="flex-fill margin-1">
                    <Label
                        className="font-sans-xs usa-label text-no-wrap"
                        htmlFor="successrate-dropdown"
                    >
                        Success Type:
                    </Label>
                    <Tooltip
                        className="fixed-tooltip"
                        label="Show only rows in one of these states."
                    >
                        <Dropdown
                            id="successrate-dropdown"
                            name="successrate-dropdown"
                            onChange={(evt) =>
                                setFilterRowSuccessState(
                                    (evt?.target?.value as SuccessRate) ||
                                        SuccessRate.UNDEFINED
                                )
                            }
                        >
                            <option value={SuccessRate.UNDEFINED}>
                                Show All
                            </option>
                            <option value={SuccessRate.ALL_FAILURE}>
                                Failed
                            </option>
                            <option value={SuccessRate.MIXED_SUCCESS}>
                                Mixed success
                            </option>
                        </Dropdown>
                    </Tooltip>
                </div>
            </form>
            <MainRender
                data={data}
                filterRowStatus={filterRowSuccessState}
                datesRange={[
                    new Date(startDate),
                    endDate ? new Date(endDate) : new Date(),
                ]}
                onDetailsClick={showDetailsModal}
            />

            <Modal
                isLarge={true}
                className="rs-compare-modal"
                ref={modalShowInfoRef}
                id={"showSuccessDetails"}
            >
                <ModalInfoRender subData={currentDataForModal} />
            </Modal>
        </section>
    );
}

export const _exportForTesting = {
    defaultStartDateIso,
    defaultEndDateIso,
    TimeSlots,
    roundIsoDateDown,
    roundIsoDateUp,
    SuccessRateTracker,
    SuccessRate,
    durationFormatShort,
    dateShortFormat,
    makeDictionary,
    MainRender,
};
