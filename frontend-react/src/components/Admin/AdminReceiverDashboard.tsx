import { useResource } from "rest-hooks";
import React, { useCallback, useMemo, useRef, useState } from "react";
import {
    Button,
    DateRangePicker,
    Dropdown,
    Grid,
    GridContainer,
    Label,
    Modal,
    ModalRef,
    ModalToggleButton,
    SiteAlert,
    TextInput,
    Tooltip,
} from "@trussworks/react-uswds";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";
import moment from "moment";

import {
    AdmConnStatusResource,
    AdmConnStatusDataType,
} from "../../resources/AdmConnStatusResource";
import { formatDate } from "../../utils/misc";
import { StyleClass } from "../Table/TableFilters";

const DAY_BACK_DEFAULT = 3 - 1; // N days (-1 because we add a day later for ranges)
const SKIP_HOURS = 2; // hrs - should be factor of 24 (e.g. 12,6,4,3,2)
const MAX_DAYS = 10;
const MAX_DAYS_MS = MAX_DAYS * 24 * 60 * 60 * 1000;

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
 * NOTE: there might be missing days or slices in the dictionary.
 * This can happen when a new receiver is onboarded in the middle of the data.
 * Or if the CRON job doesn't run because of deploy or outage leaving slice holes.
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

/**
 * Declared outside of the function so React doesn't update constantly (also, time is fixed)
 * Usage: `yesterday()`
 * @param d {Date}
 * @return {string}
 */
const startOfDayIso = (d: Date) => {
    return moment(d).startOf("day").toISOString();
};

const endOfDayIso = (d: Date) => {
    return moment(d).endOf("day").toISOString();
};

const initialStartDate = () => {
    return moment().subtract(DAY_BACK_DEFAULT, "days").toDate();
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

enum MatchingFilter {
    NO_FILTER,
    FILTER_NOT_MATCHED,
    FILTER_IS_MATCHED,
}

const MATCHING_FILTER_CLASSNAME_MAP = {
    [MatchingFilter.NO_FILTER]: "",
    [MatchingFilter.FILTER_NOT_MATCHED]: "success-result-hidden",
    [MatchingFilter.FILTER_IS_MATCHED]: "",
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
const sortStatusData = (
    dataIn: AdmConnStatusDataType[]
): AdmConnStatusDataType[] => {
    const strcmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

    const sortCompare = (
        d1: AdmConnStatusDataType,
        d2: AdmConnStatusDataType
    ) => {
        // sorting by organizationName, then receiverName, then connectionCheckStartedAt
        const orgNameCmp = strcmp(d1.organizationName, d2.organizationName);
        // orgNameCmp === 0 means same
        if (orgNameCmp !== 0) {
            return orgNameCmp;
        }
        const receiverNameCmp = strcmp(d1.receiverName, d2.receiverName);
        if (receiverNameCmp !== 0) {
            return receiverNameCmp;
        }
        return strcmp(d1.connectionCheckStartedAt, d2.connectionCheckStartedAt);
    };

    // empty case
    if (dataIn.length === 0) {
        return [];
    }

    dataIn.sort(sortCompare);
    return dataIn; // we modified array in place, but this syntax is handy.
};

// PreRenderedRowComponents breaks out row status and org+reciever name into props
// so parent can more quickly filter at a higher level without changing the whole DOM
function renderAllReceiverRows(props: {
    data: AdmConnStatusDataType[];
    startDay: Date;
    endDay: Date;
    filterErrorText: string;
    onClick: (dataItems: AdmConnStatusDataType[]) => void;
}): JSX.Element[] {
    console.log("renderAllReceiverRows");
    const filterErrorText = props.filterErrorText.trim().toLowerCase();
    const perReceiverRowElements: JSX.Element[] = [];
    let perDayElements: JSX.Element[] = [];
    let sliceElements: JSX.Element[] = [];
    let visibleSliceCount = 0; // used when error filtering
    // we use double cursors (primary moves through time and secondary through data entries)
    let offset = 0;

    // loop over all receivers (each is its own row)
    while (offset < props.data.length) {
        const successForRow = new SuccessRateTracker();
        const offsetStartRow = offset; // used during render
        let currentEntry = props.data[offset];
        let currentDate = new Date(currentEntry.connectionCheckCompletedAt);
        let currentReceiver = `${currentEntry.organizationName}|${currentEntry.receiverName}`;
        let rowReceiver = currentReceiver; // used to know when we've run out of row data

        // loop over all days
        const daySlots = new TimeSlots([props.startDay, props.endDay], 24);
        for (let [daySlotStart, daySlotEnd] of daySlots) {
            const timeSlots = new TimeSlots(
                [daySlotStart, daySlotEnd],
                SKIP_HOURS
            );
            for (let [timeSlotStart, timeSlotEnd] of timeSlots) {
                const successForSlice = new SuccessRateTracker();

                let filteredSlice =
                    filterErrorText.length === 0
                        ? MatchingFilter.NO_FILTER
                        : MatchingFilter.FILTER_NOT_MATCHED;

                // iterate over DATA that might be within this range.
                // Build aggregates for success, needed for layout.
                // NOTE: there might NOT BE ANY data for this slice...
                //   OR: there might be MULTIPLE matches within this time slice!
                while (
                    offset < props.data.length &&
                    dateIsInRange(currentDate, [timeSlotStart, timeSlotEnd]) &&
                    rowReceiver === currentReceiver
                ) {
                    // because we're moving two cursors through data (date slots and data results),
                    // we use the data results from the prior iteration when we moved that cursor forward.
                    const wasSuccessful =
                        currentEntry.connectionCheckSuccessful;
                    successForSlice.updateState(wasSuccessful);
                    successForRow.updateState(wasSuccessful);

                    // if we're doing an error search, then we want to hide none matching slices.
                    // Don't bother comparing if filterErrorText is empty
                    if (filteredSlice !== MatchingFilter.FILTER_IS_MATCHED) {
                        filteredSlice =
                            filterErrorText.length === 0
                                ? MatchingFilter.NO_FILTER
                                : currentEntry.connectionCheckResult
                                      .toLowerCase()
                                      .includes(filterErrorText)
                                ? MatchingFilter.FILTER_IS_MATCHED
                                : MatchingFilter.FILTER_NOT_MATCHED;

                        if (
                            filteredSlice !== MatchingFilter.FILTER_NOT_MATCHED
                        ) {
                            visibleSliceCount++;
                        }
                    }

                    // next entry
                    offset++;
                    if (offset >= props.data.length) {
                        break;
                    }
                    currentEntry = props.data[offset];
                    currentDate = new Date(
                        currentEntry.connectionCheckCompletedAt
                    );
                    currentReceiver = `${currentEntry.organizationName}|${currentEntry.receiverName}`;
                }

                // <editor-fold desc="Time slices for a given day render">
                const sliceClassName =
                    SUCCESS_RATE_CLASSNAME_MAP[successForSlice.currentState];

                const sliceFilterClassName =
                    MATCHING_FILTER_CLASSNAME_MAP[filteredSlice];

                // it's possible to match more than on data entry per slice
                const dataCount =
                    successForSlice.countSuccess + successForSlice.countFailed;
                const dataOffset = offset - dataCount;
                const dataOffsetEnd = offset;

                sliceElements.push(
                    <Grid
                        row
                        key={`slice:${currentReceiver}|${timeSlotStart}`}
                        className={`slice ${sliceClassName} ${sliceFilterClassName}`}
                        data-offset={dataOffset}
                        data-offset-end={dataOffsetEnd}
                        onClick={
                            successForSlice.currentState ===
                            SuccessRate.UNDEFINED
                                ? undefined // do not even install a click handler noop
                                : (evt) => {
                                      // get saved offset from "data-offset" attribute on this element
                                      const target = evt.currentTarget;
                                      const dataOffset = parseInt(
                                          target?.dataset["offset"] || "-1"
                                      );
                                      let dataOffsetEnd = parseInt(
                                          target?.dataset["offsetEnd"] || "-1"
                                      );
                                      // sanity check it's within range (should never happen)
                                      if (
                                          dataOffset >= 0 &&
                                          dataOffset < props.data.length
                                      ) {
                                          // should never happen, but safety
                                          dataOffsetEnd =
                                              dataOffsetEnd > dataOffset
                                                  ? dataOffsetEnd
                                                  : dataOffset;
                                          const subsetData = props.data.slice(
                                              dataOffset,
                                              dataOffsetEnd
                                          );
                                          props.onClick(subsetData);
                                      }
                                  }
                        }
                    >
                        {" "}
                    </Grid>
                );
                // </editor-fold>
            } // per-day time slice loop

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
        // we saved the start of this block of data, grab the information from there.
        const orgName = props.data[offsetStartRow].organizationName;
        const recvrName = props.data[offsetStartRow].receiverName;
        const combinedName = `${orgName} ${recvrName}`.toLowerCase();
        const successRate = Math.round(
            (100 * successForRow.countSuccess) /
                (successForRow.countSuccess + successForRow.countFailed)
        );
        const titleClassName =
            SUCCESS_RATE_CLASSNAME_MAP[successForRow.currentState];

        // if we have error text, then check if we filtered out all the slices
        const allSlicesFiltered = filterErrorText.length
            ? visibleSliceCount === 0
            : false;

        // cheat confession: using `data-` props allow us to stick properties
        // on components without type checking requiring it be a formal prop.
        perReceiverRowElements.push(
            <Grid
                row
                key={`perreceiver-${combinedName}-${perReceiverRowElements.length}`}
                className={"perreceiver-row"}
                data-rowstatus={successForRow.currentState}
                data-orgrecvname={combinedName}
                data-allslicesfiltered={allSlicesFiltered}
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
        perDayElements = [];
        visibleSliceCount = 0;
        // </editor-fold>
    } // while
    return perReceiverRowElements;
}

/**
 * Some filtering doesn't need to modify rows, just show/hide rows.
 * This function loops over "pre-rendered" rows and decide which to
 * include.
 */
function FilterRenderedRows(props: {
    renderedRows: JSX.Element[];
    filterRowStatus: SuccessRate;
    filterRowReceiver: string;
    filterErrorText: string;
    onClick: (dataItem: AdmConnStatusDataType[]) => void;
}) {
    const filterRowStatus = props.filterRowStatus.toUpperCase(); // enum is uppercased
    const filterRowReceiver = props.filterRowReceiver.toLowerCase();
    const filterErrorText = props.filterErrorText.trim().toUpperCase();
    const renderedRows = props.renderedRows;

    const resultArray: JSX.Element[] = [];
    for (let offset = 0; offset < renderedRows.length; offset++) {
        const renderedRow = renderedRows[offset];
        const rowStatus = renderedRow.props["data-rowstatus"] || "";
        const orgRecvName = renderedRow.props["data-orgrecvname"] || "";
        const allSlicesFilteredOut =
            renderedRow.props["data-allslicesfiltered"] || false;

        // filters remove rows.
        const hideRowBasedOnStatus =
            filterRowStatus !== SuccessRate.UNDEFINED
                ? rowStatus !== filterRowStatus
                : false;

        const hideRowBasedOnRecv =
            filterRowReceiver !== ""
                ? !orgRecvName.includes(filterRowReceiver)
                : false;

        // this filter removed ALL slices from the day, so hide the row
        const hideRowBasedOnErrorFilter =
            filterErrorText !== "" ? allSlicesFilteredOut : false;

        const hideRow =
            hideRowBasedOnStatus ||
            hideRowBasedOnRecv ||
            hideRowBasedOnErrorFilter;

        if (!hideRow) {
            resultArray.push(renderedRow);
        }
    }

    if (resultArray.length === 0) {
        return <>No rows matching time range and any filters</>;
    }

    return <>{resultArray}</>;
}

function MainRender(props: {
    data: AdmConnStatusDataType[];
    datesRange: DatePair;
    filterRowStatus: SuccessRate;
    filterErrorText: string;
    filterRowReceiver: string;
    onDetailsClick: (subData: AdmConnStatusDataType[]) => void;
}) {
    const onClick = useCallback(
        (dataItems: AdmConnStatusDataType[]) => {
            // in theory, there might be multiple events for the block, but we're only handling one for now.
            props.onDetailsClick(dataItems);
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [props.onDetailsClick]
    );
    // Example: if we decide to filter data[1], then we filter renderedRows[1]
    // this prevents the expensive row renders when a filter happens
    // (since they filter out WHOLE rows).
    const startDay = props.datesRange[0];
    const endDay = props.datesRange[1];
    const renderedRows = useMemo(
        () =>
            renderAllReceiverRows({
                data: props.data,
                startDay,
                endDay,
                filterErrorText: props.filterErrorText,
                onClick,
            }),
        // memo cannot track date changes correctly, leave them out of deps!
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [props.data, props.filterErrorText]
    );

    if (renderedRows.length === 0) {
        return <div>No Data</div>;
    }

    return (
        //
        <ScrollSync horizontal enabled>
            <GridContainer className={"rs-admindash-component"}>
                <FilterRenderedRows
                    renderedRows={renderedRows}
                    filterRowStatus={props.filterRowStatus}
                    filterRowReceiver={props.filterRowReceiver}
                    filterErrorText={props.filterErrorText}
                    onClick={onClick}
                />
            </GridContainer>
        </ScrollSync>
    );
}

function ModalInfoRender(props: { subData: AdmConnStatusDataType[] }) {
    if (!props?.subData.length) {
        return <>No Data Found</>;
    }

    const duration = (dataItem: AdmConnStatusDataType) => {
        return durationFormatShort(
            new Date(dataItem.connectionCheckCompletedAt),
            new Date(dataItem.connectionCheckStartedAt)
        );
    };

    return (
        <GridContainer className={"rs-admindash-modal"}>
            {props.subData.map((dataItem) => (
                <Grid
                    key={`dlog-item-${dataItem.receiverConnectionCheckResultId}`}
                >
                    <Grid className={"modal-info-title"}>
                        Results for connection verification check
                    </Grid>
                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label"}>Org:</Grid>
                        <Grid className={"modal-info-value"}>
                            {dataItem.organizationName} (id:{" "}
                            {dataItem.organizationId})
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
                            {dataItem.connectionCheckSuccessful
                                ? "success"
                                : "failed"}
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
                        <Grid className={"modal-info-label"}>
                            Time to complete:
                        </Grid>
                        <Grid className={"modal-info-value"}>
                            {duration(dataItem)}
                            <br />
                        </Grid>
                    </Grid>

                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label"}>
                            Result message:
                        </Grid>
                        <Grid className={"modal-info-value"}>
                            {dataItem.connectionCheckResult}
                        </Grid>
                    </Grid>
                </Grid>
            ))}
        </GridContainer>
    );
}

/**
 * We want to control date picking better than the default control allows.
 * Reasons:
 *  - We limit the max range allowed to limit large data requests.
 *  - We want start AND end picked before the expensive fetch.
 *  - Picker fields are LARGE and take up a bunch of space.
 */
function DateRangePickingAtomic(props: {
    defaultStartDate: string;
    defaultEndDate: string;
    onChange: (props: { startDate: string; endDate: string }) => void;
}) {
    const [startDate, setStartDate] = useState<string>(props.defaultStartDate);
    const [endDate, setEndDate] = useState<string>(props.defaultEndDate); // may be null because it's optional
    const modalRef = useRef<ModalRef>(null);

    // for readability
    const isDateRangeOk = useCallback(() => {
        const msEnd =
            endDate !== "" ? new Date(endDate).getTime() : new Date().getTime();
        const msStart = new Date(startDate).getTime();
        return msEnd - msStart < MAX_DAYS_MS;
    }, [startDate, endDate]);

    const formatDateFromString = (d: string) => {
        if (d === "") return "now";
        return new Date(d).toLocaleDateString();
    };

    return (
        <>
            <span className={""}>
                🗓 {formatDateFromString(startDate)} -{" "}
                {formatDateFromString(endDate)}
            </span>
            <ModalToggleButton
                modalRef={modalRef}
                opener
                className="padding-1 margin-1 usa-button--outline"
            >
                Change...
            </ModalToggleButton>
            <Modal ref={modalRef} id={"date-range-picker"}>
                <div>Select date range to show. (Max 10 days span)</div>
                <DateRangePicker
                    className={`${StyleClass.DATE_CONTAINER} margin-bottom-5`}
                    startDateLabel="From (Start Range):"
                    startDatePickerProps={{
                        id: "start-date",
                        name: "start-date-picker",
                        defaultValue: startDate,
                        onChange: (s) => {
                            if (s) {
                                setStartDate(startOfDayIso(new Date(s)));
                            }
                        },
                    }}
                    endDateLabel="Until (End Range):"
                    endDatePickerProps={{
                        id: "end-date",
                        name: "end-date-picker",
                        defaultValue: props.defaultEndDate,
                        onChange: (s) => {
                            if (s) {
                                setEndDate(endOfDayIso(new Date(s)));
                            }
                        },
                    }}
                />
                <Button
                    type="button"
                    disabled={!isDateRangeOk()}
                    onClick={() => {
                        modalRef.current?.toggleModal(undefined, false);
                        props.onChange({ startDate, endDate });
                    }}
                >
                    Update
                </Button>
                {isDateRangeOk() ? null : (
                    <span className={"rs-admindash-warn-font"}>
                        Dates are too far apart (too many days)
                    </span>
                )}
            </Modal>
        </>
    );
}

export function AdminReceiverDashboard() {
    const [startDate, setStartDate] = useState<string>(
        startOfDayIso(initialStartDate())
    );
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

    const showDetailsModal = useCallback((subData: AdmConnStatusDataType[]) => {
        if (subData.length) {
            setCurrentDataForModal(subData);
            modalShowInfoRef?.current?.toggleModal(undefined, true);
        }
    }, []);

    const data = useMemo(() => sortStatusData(results), [results]);

    return (
        <section className="grid-container">
            <h4>Receiver Status Dashboard</h4>
            <section>
                CRON job results that check if receivers are working.
                <br />
                Each slot is a 2hr time slice. Colored slots had a check run.
                Clicking on a slot shows details.
            </section>
            <SiteAlert variant="info">
                {
                    "Times shown are in YOUR LOCAL timezone. Be aware that receivers and servers may be in different zones."
                }
            </SiteAlert>
            <form autoComplete="off" className="grid-row margin-0">
                <div className="flex-auto margin-1">
                    <Label
                        className="font-sans-xs usa-label text-no-wrap"
                        htmlFor="input_filter_receivers"
                    >
                        Date range:
                    </Label>
                    <DateRangePickingAtomic
                        defaultStartDate={startOfDayIso(initialStartDate())}
                        defaultEndDate={""}
                        onChange={(props) => {
                            setStartDate(props.startDate);
                            setEndDate(props.endDate);
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
                datesRange={[
                    new Date(startDate),
                    endDate
                        ? new Date(endOfDayIso(new Date(endDate)))
                        : new Date(endOfDayIso(new Date())),
                ]}
                filterRowStatus={filterRowSuccessState}
                filterErrorText={filterErrorResults}
                filterRowReceiver={filterReceivers}
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
    startOfDayIso,
    endOfDayIso,
    initialStartDate,
    TimeSlots,
    SuccessRateTracker,
    SuccessRate,
    durationFormatShort,
    dateShortFormat,
    sortStatusData,
    MainRender,
    ModalInfoRender,
};
