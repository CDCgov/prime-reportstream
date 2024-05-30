import { addMinutes, eachDayOfInterval, interval } from "date-fns";
import { AdmConnStatusDataType } from "../../resources/AdmConnStatusResource";
import { DatePair } from "../../utils/DateTimeUtils";

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

/**
 * build the dictionary with a special path+key
 * @param dataIn
 */
export const sortStatusData = <
    T extends AdmConnStatusDataType[] | RSReceiverStatusParsed[],
>(
    dataIn: T,
): T => {
    const data = structuredClone(dataIn);

    // sorting by organizationName, then receiverName, then connectionCheckStartedAt
    data.sort(
        (
            d1: AdmConnStatusDataType | RSReceiverStatusParsed,
            d2: AdmConnStatusDataType | RSReceiverStatusParsed,
        ) => {
            // ideally the shape of this type will change so that all receivers, regardless if any status data was found, will get sorted
            const [sortStrA, sortStrB] = [d1, d2].map(
                (x) =>
                    `${x.organizationName}${x.receiverName}${new Date(x.connectionCheckStartedAt || Date.now()).toISOString()}`,
            );

            if (sortStrA > sortStrB) return 1;
            else if (sortStrA < sortStrB) return -1;

            return 0;
        },
    );

    return data;
};

export interface TimePeriodData {
    start: Date;
    end: Date;
    success: number;
    fail: number;
    isResultFilterMatch: boolean;
    successRateType: SuccessRate;
    matchingFilter: MatchingFilter;
    entries: RSReceiverStatusParsed[];
}

export function createStatusTimePeriodData({
    data,
    range: [startDate, endDate],
    filterResultMessage = "",
    timePeriodMinutes = 2 * 60,
}: {
    data: AdmConnStatusDataType[];
    range: DatePair;
    filterResultMessage?: string;
    timePeriodMinutes?: number;
}) {
    const inter = interval(startDate, endDate, { assertPositive: true });
    const numTimePeriodsPerDay = (24 * 60) / timePeriodMinutes;
    if (numTimePeriodsPerDay % 1 !== 0)
        throw new Error("Invalid time period duration");

    const timePeriodLabels = Array.from(Array(numTimePeriodsPerDay).keys()).map(
        (_, i) =>
            addMinutes(
                startDate,
                (i + 1) * timePeriodMinutes,
            ).toLocaleTimeString(),
    );
    const sortedData = sortStatusData(data).map((d) => ({
        ...d,
        connectionCheckCompletedAt: new Date(d.connectionCheckCompletedAt),
        connectionCheckStartedAt: new Date(d.connectionCheckStartedAt),
    }));
    const receiverIds = Array.from(new Set(data.map((d) => d.receiverId)));

    return receiverIds.map((id) => {
        const entries = sortedData.filter((d) => d.receiverId === id);
        const { organizationName, receiverName } = entries[0];
        const days = eachDayOfInterval(inter).map((day) => {
            const dayEntries = entries.filter(
                (e) =>
                    e.connectionCheckCompletedAt.toLocaleDateString() ===
                    day.toLocaleDateString(),
            );
            const dayString = day.toLocaleDateString();
            const timePeriods = timePeriodLabels.map((time) => {
                const start = new Date(`${day.toLocaleDateString()}, ${time}`);
                const end = addMinutes(start, timePeriodMinutes);

                const timePeriodEntries = dayEntries.filter((e) => {
                    return (
                        e.connectionCheckCompletedAt >= start &&
                        e.connectionCheckCompletedAt < end
                    );
                });
                const agg = timePeriodEntries.reduce(
                    (
                        agg,
                        { connectionCheckSuccessful, connectionCheckResult },
                    ) => {
                        if (connectionCheckSuccessful) agg.success += 1;
                        else {
                            agg.fail += 1;
                            if (
                                connectionCheckResult
                                    .toLowerCase()
                                    .includes(filterResultMessage)
                            ) {
                                agg.isResultFilterMatch = true;
                            }
                        }
                        return agg;
                    },
                    {
                        success: 0,
                        fail: 0,
                        isResultFilterMatch: false as boolean,
                    },
                );

                const successRateType =
                    agg.success && agg.fail
                        ? SuccessRate.MIXED_SUCCESS
                        : !agg.success && !agg.fail
                          ? SuccessRate.UNDEFINED
                          : agg.success
                            ? SuccessRate.ALL_SUCCESSFUL
                            : SuccessRate.ALL_FAILURE;
                const matchingFilter = !filterResultMessage
                    ? MatchingFilter.NO_FILTER
                    : filterResultMessage && agg.isResultFilterMatch
                      ? MatchingFilter.FILTER_IS_MATCHED
                      : MatchingFilter.FILTER_NOT_MATCHED;

                return {
                    start: start,
                    end: end,
                    ...agg,
                    successRateType,
                    matchingFilter,
                    entries: timePeriodEntries,
                };
            });
            return {
                dayString,
                timePeriods,
                entries: dayEntries,
            };
        });

        const { success, fail } = days.reduce(
            (agg, curr) => {
                curr.timePeriods.forEach((c) => {
                    agg.fail += c.fail;
                    agg.success += c.success;
                });
                return agg;
            },
            { success: 0, fail: 0 },
        );
        const successRate =
            !!success || !!fail
                ? Math.round((100 * success) / (success + fail))
                : 0;
        const successRateType =
            !success && !fail
                ? SuccessRate.UNDEFINED
                : successRate === 0
                  ? SuccessRate.ALL_FAILURE
                  : successRate === 100
                    ? SuccessRate.ALL_SUCCESSFUL
                    : SuccessRate.MIXED_SUCCESS;

        return {
            organizationName,
            receiverName,
            id,
            successRate,
            successRateType,
            days,
        };
    });
}

export type RSReceiverStatusParsed = Omit<
    AdmConnStatusDataType,
    "connectionCheckStartedAt" | "connectionCheckCompletedAt"
> & {
    connectionCheckStartedAt: Date;
    connectionCheckCompletedAt: Date;
};
