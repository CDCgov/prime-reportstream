import moment from "moment";

import { ResponseType, TestResponse } from "../resources/TestResponse";

import {
    formatDateWithoutSeconds,
    generateDateTitles,
    isDateExpired,
} from "./DateTimeUtils";

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

        if (submissionDate) {
            // This will fail if you change the dummy object's dates!
            expect(submissionDate.dateString).toBe("7 Apr 1970");
            expect(submissionDate.timeString).toMatch(/\d{1,2}:\d{2}/);
        } else {
            throw new Error(
                "You were the chosen one! You were meant to destroy the nulls, not join them!",
            );
        }
    });
});

describe("generateDateTitles", () => {
    test("returns null for invalid date strings", () => {
        const dateTimeData = generateDateTitles("I have the high ground!");
        expect(dateTimeData).toBe(null);
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
        const now = moment().add(1, "day").toISOString();
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
