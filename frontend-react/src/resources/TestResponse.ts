import ActionDetailsResource from "./ActionDetailsResource";

export enum ResponseType {
    ACTION_DETAIL = "actionDetail",
}

export class TestResponse {
    /* 
        We should ultimately make `data` a generic that extends
        our base class (for rest-hooks, Resource). This will do
        for now.
    */
    data: any;

    constructor(responseType: ResponseType) {
        switch (responseType) {
            case ResponseType.ACTION_DETAIL:
                this.data = this.actionDetails;
                break;
            default:
                this.data = null;
                break;
        }
    }

    actionDetails: ActionDetailsResource = {
        submissionId: 12345,
        timestamp: "1970-04-07T16:26:14.345Z",
        sender: "Jest",
        httpStatus: 201,
        externalName: "SubmissionDetails Unit Test",
        id: "x0000xx0-0xx0-0000-0x00-00x000x0x000",
        destinations: [
            {
                organization_id: "jest",
                organization: "React Unit Testing",
                service: "QUALITY_PASS",
                filteredReportRows: [
                    "For ignore.QUALITY_PASS, filter matches[ordering_facility_county, QUALITY_PASS] filtered out item 682740 at index 1",
                    "For ignore.QUALITY_PASS, filter matches[ordering_facility_county, QUALITY_PASS] filtered out item 496898 at index 3",
                ],
                sending_at: "1970-04-07T16:26:14.345Z",
                itemCount: 3,
                sentReports: [],
            },
        ],
        errors: [
            {
                scope: "",
                type: "",
                message: "",
                index: 0,
                trackingId: "",
            },
        ],
        warnings: [
            {
                scope: "",
                type: "",
                message: "",
            },
        ],
        topic: "TEST",
        warningCount: 0,
        errorCount: 0,

        /* Added because rest-hooks made me */
        pk: function (): string {
            throw new Error("Function not implemented.");
        },
        url: "",
    };
}
