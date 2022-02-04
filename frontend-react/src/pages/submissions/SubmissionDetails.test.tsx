import { render, screen } from "@testing-library/react";
import ActionDetailsResource from "../../resources/ActionDetailsResource";
import SubmissionDetails from './SubmissionDetails'

describe("Basic render tests", () => {
    const mockAPIResponse: ActionDetailsResource = {
        submissionId: 12345,
        submittedAt: "",
        submitter: "",
        httpStatus: 201,
        externalName: "",
        id: "",
        destinations: [
            {
                organization_id: "",
                organization: "",
                service: "",
                filteredReportRows: [],
                sending_at: "",
                itemCount: 0,
                sentReports: []
            }
        ],
        errors: [
            {
                scope: "",
                type: "",
                message: "",
                index: 0,
                trackingId: ""
            }
        ],
        warnings: [
            {
                scope: "",
                type: "",
                message: ""
            }
        ],
        topic: "",
        warningCount: 0,
        errorCount: 0,

        /* Added because rest-hooks made me */
        pk: function (): string {
            throw new Error("Function not implemented.");
        },
        url: ""
    }

    /* 
        TODO:
        - Figure out how to stop network call and replace with mock data
        - Test to ensure Title renders properly
        - Test to ensure Destinations render properly
    */
})