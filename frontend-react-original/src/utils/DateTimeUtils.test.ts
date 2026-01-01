import { addDays } from "date-fns";
import {
    dateShortFormat,
    durationFormatShort,
    formatDateWithoutSeconds,
    generateDateTitles,
    isDateExpired,
} from "./DateTimeUtils";
import { ResponseType, TestResponse } from "../resources/TestResponse";

/*
    Ensuring formatting of SubmissionDate type
    Dates: 1 Jan 2000
    Times: 12:00 AM
*/
describe("submission details date display", () => {
    test("SubmissionDates have valid format", () => {
        const actionDetailsTestResource = new TestResponse(
            ResponseType.ACTION_DETAIL,
        ).data;
        const submissionDate = generateDateTitles(
            actionDetailsTestResource.timestamp,
        );

        // This will fail if you change the dummy object's dates!
        expect(submissionDate?.dateString).toBe("7 Apr 1970");
        expect(submissionDate?.timeString).toMatch(/\d{1,2}:\d{2}/);
    });
});

describe("generateDateTitles", () => {
    test("returns null for invalid date strings", () => {
        const dateTimeData = generateDateTitles("I have the high ground!");
        expect(dateTimeData).toStrictEqual({
            dateString: "N/A",
            timeString: "N/A",
        });
    });

    test("does not error for dates with single digit minutes", () => {
        const dateTimeData = generateDateTitles("2022-07-25T16:09:00.000Z");
        expect(dateTimeData).not.toBe(null);
        // checking precise return value here is hard due to dependency on time zone
        // where test is being run
        // // expect(dateTimeData?.timeString).toEqual("16:09");
    });
});

describe("isDateExpired", () => {
    test("returns true if date has expired", () => {
        const expiredDateTime = isDateExpired("2023-02-09T22:24:01.938Z");
        expect(expiredDateTime).toBeTruthy();
    });

    test("returns false if date has not expired", () => {
        const now = addDays(new Date(), 1).toISOString();
        const futureDateTime = isDateExpired(now);
        expect(futureDateTime).toBeFalsy();
    });
});

describe("formatDateWithoutSeconds", () => {
    test("returns a formatted date without seconds", () => {
        const date = formatDateWithoutSeconds("2023-02-09T22:24:01.938Z");
        expect(date).toBe("2/9/2023 10:24 PM");
    });

    test("returns today's date formatted without seconds", () => {
        const expectedDate = new Date().toLocaleString([], {
            year: "numeric",
            month: "numeric",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
        const date = formatDateWithoutSeconds("");
        expect(date).toBe(expectedDate.replace(/,/, ""));
    });
});

describe("dateShortFormat", () => {
    test("as expected", () => {
        expect(dateShortFormat(new Date("2022-07-11T08:09:22.748Z"))).toBe(
            "Mon, 7/11/2022",
        );
    });
});

describe("durationFormatShort", () => {
    test("as expected", () => {
        const now = new Date();
        const before = new Date(now);
        before.setHours(
            before.getHours() - 1,
            before.getMinutes() - 2,
            before.getSeconds() - 3,
        );

        const result1 = durationFormatShort(now, before);
        expect(result1).toBe("1h 02m 03s");

        const future2 = new Date(now.getTime() + 5678);
        const result2 = durationFormatShort(future2, now);
        expect(result2).toBe("05.678s");

        future2.setHours(future2.getHours() + 12, future2.getMinutes() + 34);
        const result3 = durationFormatShort(future2, now);
        expect(result3).toBe("12h 34m 05.678s");

        const result4 = durationFormatShort(now, now);
        expect(result4).toBe("");
    });
});
