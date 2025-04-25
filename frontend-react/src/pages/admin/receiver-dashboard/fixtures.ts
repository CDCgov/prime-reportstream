import { createStatusTimePeriodData, RSReceiverStatusParsed } from "./utils";
import type { RSReceiverStatus } from "../../../hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus";
import type { DatePair } from "../../../utils/DateTimeUtils";

export const mockDateRange = [new Date("2022-07-11"), new Date("2022-07-13")] as DatePair;
export const mockReceiversStatuses = [
    {
        receiverConnectionCheckResultId: 2397,
        organizationId: 61,
        receiverId: 313,
        connectionCheckResult: "connectionCheckResult dummy result 2397",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T00:00:00.001Z",
        connectionCheckCompletedAt: "2022-07-11T00:00:50.000Z",
        organizationName: "ca-dph",
        receiverName: "elr",
    },
    {
        receiverConnectionCheckResultId: 2398,
        organizationId: 61,
        receiverId: 314,
        connectionCheckResult: "connectionCheckResult dummy result 2398",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T07:59:24.036Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:24.358Z",
        organizationName: "ca-dph",
        receiverName: "elr-secondary",
    },
    {
        receiverConnectionCheckResultId: 2399,
        organizationId: 49,
        receiverId: 315,
        connectionCheckResult: "connectionCheckResult dummy result 2399",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T07:59:23.713Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:24.033Z",
        organizationName: "oh-doh",
        receiverName: "elr",
    },
    {
        // this entry is out-of-sort-order and connectionCheckSuccessful:failed
        receiverConnectionCheckResultId: 2396,
        organizationId: 61,
        receiverId: 313,
        connectionCheckResult: "connectionCheckResult dummy result 2396",
        connectionCheckSuccessful: false,
        connectionCheckStartedAt: "2022-07-11T07:59:23.385Z",
        connectionCheckCompletedAt: "2022-07-11T07:59:23.711Z",
        organizationName: "ca-dph",
        receiverName: "elr",
    },
    {
        receiverConnectionCheckResultId: 2395,
        organizationId: 46,
        receiverId: 316,
        connectionCheckResult: "connectionCheckResult dummy result 2395",
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: "2022-07-11T08:09:23.066Z",
        connectionCheckCompletedAt: "2022-07-11T08:09:23.383Z",
        organizationName: "vt-doh",
        receiverName: "elr-otc",
    },
    {
        receiverConnectionCheckResultId: 2394,
        organizationId: 46,
        receiverId: 317,
        connectionCheckResult: "connectionCheckResult dummy result 2394",
        connectionCheckSuccessful: false,
        connectionCheckStartedAt: "2022-07-11T08:09:22.748Z",
        connectionCheckCompletedAt: "2022-07-11T08:09:23.063Z",
        organizationName: "vt-doh",
        receiverName: "elr-secondary",
    },
] satisfies RSReceiverStatus[];

export const mockReceiversStatusesParsed = mockReceiversStatuses.map((s) => ({
    ...s,
    connectionCheckCompletedAt: new Date(s.connectionCheckCompletedAt),
    connectionCheckStartedAt: new Date(s.connectionCheckStartedAt),
})) satisfies RSReceiverStatusParsed[];

export const mockReceiversStatusesTimePeriod = createStatusTimePeriodData({
    data: mockReceiversStatuses,
    range: mockDateRange,
});
