import { ResponseType, TestResponse } from "../resources/TestResponse";

import { generateSubmissionDate } from "./DateTimeUtils";

/*
    Ensuring formatting of SubmissionDate type
    Dates: 1 Jan 2000
    Times: 12:00 AM
*/
test("SubmissionDates have valid format", () => {
    const actionDetailsTestResource = new TestResponse(
        ResponseType.ACTION_DETAIL
    ).data;
    const submissionDate = generateSubmissionDate(
        actionDetailsTestResource.submittedAt
    );

    if (submissionDate) {
        // This will fail if you change the dummy object's dates!
        expect(submissionDate.dateString).toBe("7 Apr 1970");
        expect(submissionDate.timeString).toMatch(
            /\d{1,2}:\d{1,2} [A,P]M/
        );
    } else {
        throw new Error(
            "You were the chosen one! You were meant to destroy the nulls, not join them!"
        );
    }
});

test("SubmissionDate returns null for invalid date strings", () => {
    const submissionDate = generateSubmissionDate("I have the high ground!");
    expect(submissionDate).toBe(null);
});

test("SubmissionDate returns null for invalid date numbers", () => {
    const submissionDate = generateSubmissionDate("-1");
    expect(submissionDate).toBe(null);
});
