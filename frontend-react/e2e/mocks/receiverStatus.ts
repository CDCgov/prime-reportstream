import { addDays, addMinutes, endOfDay, startOfDay, subDays } from "date-fns"
import { randomInt } from "crypto"
import { writeFileSync } from "fs"
import { SKIP_HOURS } from "../../src/components/Admin/AdminReceiverDashboard.constants"
import { TimeSlots } from "../../src/utils/DateTimeUtils"

const mockFailResult = "ERROR"
const mockSuccessResult = "SUCCESS"


export function createMockGetReceiverStatus([start, end]: [start?: Date, end?: Date] = []) {
  // pad by an extra day on both ends to cover any time zone
  if(!start) start = subDays(startOfDay(new Date()), 3);
  if(!end) end = addDays(endOfDay(new Date()), 1);
  const orgA = {organizationId: 1, organizationName: "org-a"}
  // Divide periods by this amount to select randomly for when a status event occurs
  const innerPeriodMinutes = 6;
  // ensure an entry for every hour
  const statusIntervalMinutes = 60;
  const dates = new TimeSlots([start, end], 24 * 60).toArray();
  const dateTimePeriods = dates.map(range => new TimeSlots(range, 60).toArray())

  // all undefined 
  // SHOULD be possible if the endpoint is improved to return all receivers
  // in addition to date range matching status entries

  // all success
  const allSuccess = dates.flatMap((_,i) => dateTimePeriods[i].map(([start, end]) => {
    const startedAtPeriod = new TimeSlots([start, end], 6)
        .drop(randomInt(statusIntervalMinutes / innerPeriodMinutes))
        .next().value!;
    const completedAt = addMinutes(startedAtPeriod[0], 1).toISOString();

    return {
        ...orgA,
        receiverConnectionCheckResultId: randomInt(3000),
        connectionCheckResult: mockSuccessResult,
        connectionCheckSuccessful: true,
        connectionCheckStartedAt: startedAtPeriod[0].toISOString(),
        connectionCheckCompletedAt: completedAt,
        receiverName: "all-success",
        receiverId: 1,
    };
  }))

  const allFail = dates.flatMap((_, i) =>
      dateTimePeriods[i].map(([start, end]) => {
    const startedAtPeriod = new TimeSlots([start, end], 6)
        .drop(randomInt(statusIntervalMinutes / innerPeriodMinutes))
        .next().value!;
    const completedAt = addMinutes(startedAtPeriod[0], 1).toISOString();

          return {
              ...orgA,
              receiverConnectionCheckResultId: randomInt(3000),
              connectionCheckResult: mockFailResult,
              connectionCheckSuccessful: false,
              connectionCheckStartedAt: startedAtPeriod[0].toISOString(),
              connectionCheckCompletedAt: completedAt,
              receiverName: "all-fail",
              receiverId: 2,
          };
      }),
  );

  let hasFailure = false;
  const mixed = dates.flatMap((_, i) =>
      dateTimePeriods[i].map(([start, end], i, arr) => {
          const isFail = i === arr.length - 1 && !hasFailure ? true : !!randomInt(2)
          if(isFail) hasFailure = true;

    const startedAtPeriod = new TimeSlots([start, end], 6)
        .drop(randomInt(statusIntervalMinutes / innerPeriodMinutes))
        .next().value!;
    const completedAt = addMinutes(startedAtPeriod[0], 1).toISOString();

          return {
              ...orgA,
              receiverConnectionCheckResultId: randomInt(3000),
              connectionCheckResult: isFail ? mockFailResult : mockSuccessResult,
              connectionCheckSuccessful: !isFail,
              connectionCheckStartedAt: startedAtPeriod[0],
              connectionCheckCompletedAt: completedAt,
              receiverName: "mixed",
              receiverId: 3
          };
      }),
  );

  const mixedWithUndefined = dates.flatMap((_, i) =>
      dateTimePeriods[i].map(([start, end]) => {
          const isUndefined = randomInt(3) === 2;
          if(isUndefined) return;

          const isFail = !!randomInt(2);

    const startedAtPeriod = new TimeSlots([start, end], 6)
        .drop(randomInt(statusIntervalMinutes / innerPeriodMinutes))
        .next().value!;
    const completedAt = addMinutes(startedAtPeriod[0], 1).toISOString();

          return {
              ...orgA,
              receiverConnectionCheckResultId: randomInt(3000),
              connectionCheckResult: isFail
                  ? mockFailResult
                  : mockSuccessResult,
              connectionCheckSuccessful: !isFail,
              connectionCheckStartedAt: startedAtPeriod[0],
              connectionCheckCompletedAt: completedAt,
              receiverName: "mixedWithUndefined",
              receiverId: 4,
          };
      }),
  ).filter(Boolean);

  return [allSuccess, allFail, mixed, mixedWithUndefined].flat();
}

export const MOCK_GET_RECEIVER_STATUS = []