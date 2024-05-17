import {
    Button,
    DateRangePicker,
    Grid,
    GridContainer,
    Label,
    Modal,
    ModalRef,
    ModalToggleButton,
    Select,
    SiteAlert,
    TextInput,
    Tooltip,
} from "@trussworks/react-uswds";
import { useCallback, useMemo, useRef, useState } from "react";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import { useResource } from "rest-hooks";
import { MAX_DAYS_MS, SKIP_HOURS } from "./AdminReceiverDashboard.constants";
import rawResults from "../../../debug.json"
import {
    AdmConnStatusDataType,
    AdmConnStatusResource,
} from "../../resources/AdmConnStatusResource";
import { DatePair, durationFormatShort, endOfDayIso, initialEndDate, initialStartDate, startOfDayIso, TimeSlots } from "../../utils/DateTimeUtils";
import { formatDate } from "../../utils/misc";
import { StyleClass, TableFilterDateLabel } from "../Table/TableFilters";
import { USLink } from "../USLink";


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



export enum SuccessRate {
    UNDEFINED = "UNDEFINED",
    ALL_SUCCESSFUL = "ALL_SUCCESSFUL",
    ALL_FAILURE = "ALL_FAILURE",
    MIXED_SUCCESS = "MIXED_SUCCESS",
}

export const SUCCESS_RATE_CLASSNAME_MAP = {
    [SuccessRate.UNDEFINED]: "success-undefined",
    [SuccessRate.ALL_SUCCESSFUL]: "success-all",
    [SuccessRate.ALL_FAILURE]: "failure-all",
    [SuccessRate.MIXED_SUCCESS]: "success-mixed",
};

export enum MatchingFilter {
    NO_FILTER,
    FILTER_NOT_MATCHED,
    FILTER_IS_MATCHED,
}

export const MATCHING_FILTER_CLASSNAME_MAP = {
    [MatchingFilter.NO_FILTER]: "",
    [MatchingFilter.FILTER_NOT_MATCHED]: "success-result-hidden",
    [MatchingFilter.FILTER_IS_MATCHED]: "",
};

export const strcmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

/**
 * build the dictionary with a special path+key
 * @param dataIn
 */
export const sortStatusData = (
    dataIn: AdmConnStatusDataType[],
): AdmConnStatusDataType[] => {
    const data = structuredClone(dataIn)
    // empty case
    if (dataIn.length === 0) {
        return [];
    }

    data.sort((d1: AdmConnStatusDataType, d2: AdmConnStatusDataType) => {
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
    });
    return data; // we modified array in place, but returning value is handy for chaining.
};

export interface TimePeriodData {
    start: Date,
    end: Date,
    success: number,
    fail: number,
    isResultFilterMatch: boolean,
    successRateType: SuccessRate,
    matchingFilter: MatchingFilter,
    entries: AdmConnStatusDataType[]
}

export function createStatusData({data, startDate, endDate, filterResultMessage}: {
    data: AdmConnStatusDataType[];
    startDate: Date;
    endDate: Date;
    filterResultMessage: string;
}) {
    const sortedData = sortStatusData(data).map(d => ({...d, connectionCheckCompletedAt: new Date(d.connectionCheckCompletedAt), connectionCheckStartedAt: new Date(d.connectionCheckStartedAt)}));
    const receiverIds = Array.from(new Set(sortedData.map(d => d.receiverId)))
    const daySlots = new TimeSlots([startDate, endDate], 24 * 60).toArray();
    const timeSlots = daySlots.map(([start, end]) => new TimeSlots([start, end], SKIP_HOURS * 60).toArray());

    return receiverIds.map(id => {
        const entries = sortedData.filter(d => d.receiverId === id);
        const {organizationName, receiverName} = entries[0]

        const days = Array.from(daySlots.entries()).map(([i, [start,]]) => {
            const dayEntries = entries.filter(e => e.connectionCheckCompletedAt.toLocaleDateString() === start.toLocaleDateString())
            const dayTimeSlots = timeSlots[i];
            const dayString = start.toLocaleDateString();
            const timePeriods = dayTimeSlots.map(([start, end]) => {
                const timePeriodEntries = dayEntries.filter(e => {
                    return e.connectionCheckCompletedAt >= start && e.connectionCheckCompletedAt < end;
                });
                const agg = timePeriodEntries.reduce((agg, {connectionCheckSuccessful, connectionCheckResult}) => {
                    if(connectionCheckSuccessful) agg.success += 1;
                    else {
                        agg.fail += 1;
                        if(connectionCheckResult
                                .toLowerCase()
                                .includes(filterResultMessage)) {
                                    agg.isResultFilterMatch = true;
                                }
                    }
                    return agg;
                }, {success: 0, fail: 0, isResultFilterMatch: false as boolean})

                const successRateType = agg.success && agg.fail ? SuccessRate.MIXED_SUCCESS : !agg.success && !agg.fail ? SuccessRate.UNDEFINED : agg.success ? SuccessRate.ALL_SUCCESSFUL : SuccessRate.ALL_FAILURE;
                const matchingFilter = !filterResultMessage ? MatchingFilter.NO_FILTER : filterResultMessage && agg.isResultFilterMatch ? MatchingFilter.FILTER_IS_MATCHED : MatchingFilter.FILTER_NOT_MATCHED;
                
                return {
                    start: start,
                    end: end,
                    ...agg,
                    successRateType,
                    matchingFilter,
                    entries: timePeriodEntries,
                    debug: `${start.toISOString()} - ${end.toISOString()}`,
                    debug2: timePeriodEntries.map(e => e.connectionCheckCompletedAt.toISOString())
                }
            });
            return {
                dayString,
                timePeriods,
                entries: dayEntries,
                debug1: dayEntries.map(d => d.connectionCheckCompletedAt.toISOString())
            }
        });

        const {success, fail} = days.reduce((agg, curr) => {
            curr.timePeriods.forEach(c => {
                agg.fail += c.fail;
                agg.success += c.success;
            })
            return agg
        }, {success: 0, fail: 0})
        const successRate = !!success || !!fail ? Math.round(
            (100 * success) /
                (success + fail),
        ) : 0;
        const successRateType = !success && !fail ? SuccessRate.UNDEFINED : successRate === 0 ? SuccessRate.ALL_FAILURE : successRate === 100 ? SuccessRate.ALL_SUCCESSFUL : SuccessRate.MIXED_SUCCESS;

        return {
            organizationName,
            receiverName,
            id,
            successRate,
            successRateType,
            days,
        }
    });
}

export function MainRender({
    datesRange: [startDate, endDate],
    filterResultMessage,
    filterRowReceiver,filterRowStatus,onDetailsClick
}: {
    datesRange: DatePair;
    filterRowStatus: SuccessRate;
    filterResultMessage: string;
    filterRowReceiver: string;
    onDetailsClick: (subData: AdmConnStatusDataType[]) => void;
}) {
    const results = useResource(AdmConnStatusResource.list(), {
        startDate: startOfDayIso(startDate),
        endDate: endOfDayIso(endDate),
    });
    //console.log(results.filter(x => x.receiverName === "all-fail").map(x => x.connectionCheckCompletedAt).toSorted())
    const data = useMemo(() => createStatusData({data: results, startDate, endDate, filterResultMessage}), [endDate, filterResultMessage, results, startDate]);

    const handleTimePeriodClick = useCallback(
        ({entries}: TimePeriodData) => {
            // in theory, there might be multiple events for the block, but we're only handling one for now.
            onDetailsClick(entries);
        },
        [onDetailsClick],
    );

    if(data.length === 0) {
        return <div>No Data</div>;
    }

    const filteredData = data.filter(d => {
        return (!filterRowReceiver || d.receiverName.toLowerCase().includes(filterRowReceiver)) && (!filterRowStatus || filterRowStatus === SuccessRate.UNDEFINED || d.successRateType === filterRowStatus)
    });

    if(filteredData.length === 0) {
        return <div>No data matching filters</div>
    }

    return (
        <ScrollSync horizontal enabled>
            <GridContainer className={"rs-admindash-component"}>
                {
                    filteredData.map(d => (
                        <Grid
                            row
                            key={`perreceiver-${d.organizationName}-${d.receiverName}`}
                            className={"perreceiver-row"}
                        >
                            <Grid className={`title-column ${SUCCESS_RATE_CLASSNAME_MAP[d.successRateType]}`}>
                                <div className={"title-text"}>
                                    <USLink href={`/admin/orgsettings/org/${d.organizationName}`}>{d.organizationName}</USLink>
                                    <br />
                                    <USLink href={`/admin/orgreceiversettings/org/${d.organizationName}/receiver/${d.receiverName}/action/edit`}>{d.receiverName}</USLink>
                                    <br />
                                    {d.successRate}%
                                </div>
                            </Grid>
                            <ScrollSyncPane enabled>
                                <Grid row className={"horizontal-scroll"}>
                                    <Grid row className={"week-column"}>
                                        {d.days.map(d => {
                                            return <GridContainer
                                                key={`perday-${d.dayString}`}
                                                className={"perday-component"}
                                            >
                                                <Grid row className={"title-row"}>
                                                    {d.dayString}
                                                </Grid>
                                                <Grid  gap={1} row className={"slices-row slices-row-12"}>
                                                {d.timePeriods.map(t => {
                                                    return (
                                                        <Grid
                                                            row
                                                            key={`slice:${t.end.toISOString()}`}
                                                            className={`slice ${SUCCESS_RATE_CLASSNAME_MAP[t.successRateType]} ${MATCHING_FILTER_CLASSNAME_MAP[t.matchingFilter]}`}
                                                            role="button"
                                                            aria-disabled={t.successRateType === SuccessRate.UNDEFINED}
                                                            onClick={
                                                                t.successRateType === SuccessRate.UNDEFINED
                                                                    ? undefined // do not even install a click handler noop
                                                                    : () => handleTimePeriodClick(t)
                                                            }
                                                        >
                                                            {" "}
                                                        </Grid>
                                                    )})}</Grid>
                                            </GridContainer>
                                        })}
                                    </Grid>
                                </Grid>
                            </ScrollSyncPane>
                        </Grid>
                    ))
                }
            </GridContainer>
        </ScrollSync>
    );
}

export function ModalInfoRender(props: { subData: AdmConnStatusDataType[] }) {
    if (!props?.subData.length) {
        return <>No Data Found</>;
    }

    const duration = (dataItem: AdmConnStatusDataType) => {
        return durationFormatShort(
            new Date(dataItem.connectionCheckCompletedAt),
            new Date(dataItem.connectionCheckStartedAt),
        );
    };

    return (
        <GridContainer className={"rs-admindash-modal-container"}>
            {/* We support multiple results per slice */}
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
export function DateRangePickingAtomic(props: {
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
            <span>
                🗓 {formatDateFromString(startDate)}
                {" — "}
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
                    startDateLabel={TableFilterDateLabel.START_DATE}
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
                    endDateLabel={TableFilterDateLabel.END_DATE}
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
                        Dates are too far apart (too many days - max 10 days)
                    </span>
                )}
            </Modal>
        </>
    );
}

export function AdminReceiverDashboard() {
    const [startDate, setStartDate] = useState<string>(
        startOfDayIso(initialStartDate()),
    );
    const [endDate, setEndDate] = useState<string>(
        initialEndDate().toISOString(),
    );
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

    return (
        <article>
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
            <form autoComplete="off" className="grid-row margin-0" name="filter">
                <div className="flex-auto margin-1">
                    <Label
                        className="font-sans-xs usa-label text-no-wrap"
                        htmlFor="input_filter_receivers"
                    >
                        Date range:
                    </Label>
                    <DateRangePickingAtomic
                        defaultStartDate={startOfDayIso(initialStartDate())}
                        defaultEndDate={initialEndDate().toISOString()}
                        onChange={(params) => {
                            setStartDate(params.startDate);
                            setEndDate(params.endDate);
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
                        <Select
                            id="successrate-dropdown"
                            name="successrate-dropdown"
                            onChange={(evt) =>
                                setFilterRowSuccessState(
                                    (evt?.target?.value as SuccessRate) ||
                                        SuccessRate.UNDEFINED,
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
                        </Select>
                    </Tooltip>
                </div>
            </form>
                    <MainRender
                        datesRange={[
                            new Date(startDate),
                            endDate
                                ? new Date(endOfDayIso(new Date(endDate)))
                                : new Date(endOfDayIso(new Date())),
                        ]}
                        filterRowStatus={filterRowSuccessState}
                        filterResultMessage={filterErrorResults
                            .trim()
                            .toLowerCase()}
                        filterRowReceiver={filterReceivers.trim().toLowerCase()}
                        onDetailsClick={showDetailsModal}
                    />
            <Modal
                isLarge={true}
                className="rs-admindash-modal"
                ref={modalShowInfoRef}
                id={"showSuccessDetails"}
            >
                <ModalInfoRender subData={currentDataForModal} />
            </Modal>
        </article>
    );
}