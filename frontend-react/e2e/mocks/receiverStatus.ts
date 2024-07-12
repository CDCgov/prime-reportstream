import {
    addMinutes,
    addSeconds,
    differenceInMinutes,
    endOfDay,
    interval,
    startOfDay,
    subDays,
} from "date-fns";
import { randomInt } from "crypto";
import { RSReceiverStatus } from "../../src/hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus";
import { SuccessRate } from "../../src/pages/admin/receiver-dashboard/utils";

const mockFailResult = "ERROR";
const mockSuccessResult = "SUCCESS";

/**
 * Create a set of mock statuses for a specific org's receiver. Default day range
 * is 2 days ago to today (UTC) if one is not provided. Statuses will
 * be created once per `maxMinutesPerStatus` minutes (default is 2 hours aka
 * 120 minutes).
 */
export function createMockGetReceiverStatusSet({
    range,
    maxMinutesPerStatus = 2 * 60,
    receiver,
    statusType = SuccessRate.MIXED_SUCCESS,
    randomlySkip,
    statusIdStart = 1,
}: {
    range?: [start: Date, end: Date];
    maxMinutesPerStatus?: number;
    statusType?:
        | SuccessRate.ALL_SUCCESSFUL
        | SuccessRate.ALL_FAILURE
        | SuccessRate.MIXED_SUCCESS;
    randomlySkip?: boolean;
    statusIdStart?: number;
    receiver: {
        organizationName: string;
        organizationId: number;
        receiverName: string;
        receiverId: number;
    };
}) {
    // ensure range is valid
    if (range) {
        interval(range[0], range[1], { assertPositive: true });
    }
    const now = new Date();
    const start = startOfDay(range ? range[0] : subDays(now, 2));
    const end = endOfDay(range ? range[1] : now);
    const statusSet: RSReceiverStatus[] = [];
    // add minute lost from endOfDay conversion
    const statusSetLength =
        (differenceInMinutes(end, start) + 1) / maxMinutesPerStatus;
    let currDateTime = start;
    let i = statusIdStart;
    // limit skips and fails to a max of a third of total possible set
    const failLength =
        statusType === SuccessRate.ALL_FAILURE
            ? statusSetLength
            : statusType === SuccessRate.MIXED_SUCCESS
              ? randomInt(3, Math.ceil(statusSetLength / 3))
              : 0;
    const skipLength = randomlySkip
        ? randomInt(3, Math.ceil(statusSetLength / 3))
        : 0;
    const skipIndexes: number[] = [];
    const failIndexes: number[] = [];

    // pick our indexes that will fail or skip
    while (failIndexes.length < failLength) {
        const i = randomInt(statusSetLength);
        if (!failIndexes.includes(i)) failIndexes.push(i);
    }
    while (skipIndexes.length < skipLength) {
        const i = randomInt(statusSetLength);
        if (!failIndexes.includes(i) && !skipIndexes.includes(i))
            skipIndexes.push(i);
    }

    // loop through time
    do {
        const normalizedI = i - statusIdStart;
        const isSkipped = skipIndexes.includes(normalizedI);
        if (!isSkipped) {
            const startedAt = currDateTime;
            const completedAt = addSeconds(startedAt, 5);
            const isSuccessful = !failIndexes.includes(normalizedI);

            statusSet.push({
                ...receiver,
                receiverConnectionCheckResultId: i,
                connectionCheckStartedAt: startedAt.toISOString(),
                connectionCheckCompletedAt: completedAt.toISOString(),
                connectionCheckSuccessful: isSuccessful,
                connectionCheckResult:
                    (isSuccessful ? mockSuccessResult : mockFailResult) +
                    ` ${i}`,
            });
        }

        i++;
        currDateTime = addMinutes(currDateTime, maxMinutesPerStatus);
    } while (currDateTime < end);

    /*console.debug({
        receiver: receiver.receiverName,
        statusType,
        randomlySkip,
        skipIndexes,
        failIndexes,
        length: statusSet.length
    })*/

    return statusSet;
}

/**
 * Generate new mock from scratch.
 */
export function createMockGetReceiverStatus(
    range?: [start: Date, end: Date],
    maxMinutesPerStatus?: number,
) {
    const orgA = { organizationId: 1, organizationName: "org-a" };

    const success = createMockGetReceiverStatusSet({
        range,
        maxMinutesPerStatus,
        receiver: {
            ...orgA,
            receiverId: 1,
            receiverName: "all-success",
        },
        statusType: SuccessRate.ALL_SUCCESSFUL,
    });

    const fail = createMockGetReceiverStatusSet({
        range,
        maxMinutesPerStatus,
        receiver: {
            ...orgA,
            receiverId: 2,
            receiverName: "all-fail",
        },
        statusType: SuccessRate.ALL_FAILURE,
        statusIdStart: 100,
    });

    const mixedWithSkips = createMockGetReceiverStatusSet({
        range,
        maxMinutesPerStatus,
        receiver: {
            ...orgA,
            receiverId: 4,
            receiverName: "mixed-with-skips",
        },
        statusIdStart: 300,
        randomlySkip: true,
    });

    // the shape of the type would have to change to account for this scenario but hacking it
    // in for now to test frontend for this scenario
    const skipped = [
        {
            ...orgA,
            receiverId: 5,
            receiverName: "skipped",
        } as unknown as RSReceiverStatus,
    ];

    const data = [success, fail, mixedWithSkips, skipped].flat();

    return data;
}
