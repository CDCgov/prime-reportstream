import { ResponseType, TestResponse } from "../resources/TestResponse";

import { generateDateTitles } from "./DateTimeUtils";

/*
    Ensuring formatting of SubmissionDate type
    Dates: 1 Jan 2000
    Times: 12:00 AM
*/
describe("submission details date display", () => {
    test("SubmissionDates have valid format", () => {
        const actionDetailsTestResource = new TestResponse(
            ResponseType.ACTION_DETAIL
        ).data;
        const submissionDate = generateDateTitles(
            actionDetailsTestResource.timestamp
        );

        if (submissionDate) {
            // This will fail if you change the dummy object's dates!
            expect(submissionDate.dateString).toBe("7 Apr 1970");
            expect(submissionDate.timeString).toMatch(/\d{1,2}:\d{2}/);
        } else {
            throw new Error(
                "You were the chosen one! You were meant to destroy the nulls, not join them!"
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
