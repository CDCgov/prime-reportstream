import { ResponseType, TestResponse } from "../resources/TestResponse";

import { generateDateTitles } from "./DateTimeUtils";

/*
    Ensuring formatting of SubmissionDate type
    Dates: 1 Jan 2000
    Times: 12:00 AM
*/
test("SubmissionDates have valid format", () => {
    const actionDetailsTestResource = new TestResponse(
        ResponseType.ACTION_DETAIL
    ).data;
    const submissionDate = generateDateTitles(
        actionDetailsTestResource.submittedAt
    );

    if (submissionDate) {
        // This will fail if you change the dummy object's dates!
        expect(submissionDate.dateString).toBe("7 Apr 1970");
        expect(submissionDate.timeString).toMatch(/\d{1,2}:\d{1,2}/);
    } else {
        throw new Error(
            "You were the chosen one! You were meant to destroy the nulls, not join them!"
        );
    }
});

test("SubmissionDate returns null for invalid date strings", () => {
    const submissionDate = generateDateTitles("I have the high ground!");
    expect(submissionDate).toBe(null);
});

test("SubmissionDate returns null for invalid date numbers", () => {
    const submissionDate = generateDateTitles("-1");
    expect(submissionDate).toBe(null);
});
