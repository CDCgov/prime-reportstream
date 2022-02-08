import ActionDetailsResource from "../resources/ActionDetailsResource";

import { generateSubmissionDate } from "./DateTimeUtils";

/* 
    Ensuring formatting of SubmissionDate type
    Dates: 1 Jan 2000
    Times: 12:00 AM
*/
test("SubmissionDates have valid format", () => {
    const actionDetailsTestResource = ActionDetailsResource.dummy();
    const submissionDate = generateSubmissionDate(
        actionDetailsTestResource.submittedAt
    );

    // This will fail if you change the dummy object's dates!
    expect(submissionDate.dateString).toBe("7 Apr 1970");
    expect(submissionDate.timeString).toMatch(/[0-9]+:[0-9]+ [a-zA-Z]M/i);
});
