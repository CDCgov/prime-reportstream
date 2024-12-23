import { Grid, GridContainer } from "@trussworks/react-uswds";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";
import { USLink } from "../../../../components/USLink";
import { dateShortFormat } from "../../../../utils/DateTimeUtils";
import {
    MATCHING_FILTER_CLASSNAME_MAP,
    ReceiverStatusTimePeriod,
    SUCCESS_RATE_CLASSNAME_MAP,
    SuccessRate,
    type TimePeriodData,
} from "../utils";

export interface ReceiversStatusesDisplayProps {
    receiverStatuses: ReceiverStatusTimePeriod[];
    onTimePeriodClick?: (timePeriod: TimePeriodData) => void;
}

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
export function ReceiversStatusesDisplay({ receiverStatuses, onTimePeriodClick }: ReceiversStatusesDisplayProps) {
    return (
        <ScrollSync horizontal enabled>
            <GridContainer className={"rs-admindash-component"}>
                {receiverStatuses.map((d) => (
                    <Grid
                        row
                        key={`perreceiver-${d.organizationName}-${d.receiverName}-${d.id}`}
                        className={"perreceiver-row"}
                    >
                        <Grid className={`title-column ${SUCCESS_RATE_CLASSNAME_MAP[d.successRateType]}`}>
                            <div className={"title-text"}>
                                <USLink href={`/admin/orgsettings/org/${d.organizationName}`}>
                                    {d.organizationName}
                                </USLink>
                                <br />
                                <USLink
                                    href={`/admin/orgreceiversettings/org/${d.organizationName}/receiver/${d.receiverName}/action/edit`}
                                >
                                    {d.receiverName}
                                </USLink>
                                <br />
                                {d.successRate}%
                            </div>
                        </Grid>
                        <ScrollSyncPane enabled>
                            <Grid row className={"horizontal-scroll"}>
                                <Grid row className={"week-column"}>
                                    {d.days.map(({ day, dayString, timePeriods }) => {
                                        return (
                                            <GridContainer key={`perday-${dayString}`} className={"perday-component"}>
                                                <Grid row className={"title-row"}>
                                                    {dateShortFormat(day)}
                                                </Grid>
                                                <Grid gap={1} row className={"slices-row slices-row-12"}>
                                                    {timePeriods.map((t) => {
                                                        return (
                                                            <Grid
                                                                row
                                                                key={`slice:${t.end.toISOString()}`}
                                                                className={`slice ${SUCCESS_RATE_CLASSNAME_MAP[t.successRateType]} ${MATCHING_FILTER_CLASSNAME_MAP[t.matchingFilter]}`}
                                                                role="button"
                                                                aria-disabled={
                                                                    t.successRateType === SuccessRate.UNDEFINED
                                                                }
                                                                onClick={
                                                                    t.successRateType === SuccessRate.UNDEFINED
                                                                        ? undefined // do not even install a click handler noop
                                                                        : () => onTimePeriodClick?.(t)
                                                                }
                                                            >
                                                                {" "}
                                                            </Grid>
                                                        );
                                                    })}
                                                </Grid>
                                            </GridContainer>
                                        );
                                    })}
                                </Grid>
                            </Grid>
                        </ScrollSyncPane>
                    </Grid>
                ))}
            </GridContainer>
        </ScrollSync>
    );
}
