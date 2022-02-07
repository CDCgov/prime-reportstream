import {
    Destination,
    ReportError,
    ReportWarning,
} from "../types/SubmissionDetailsTypes";

import AuthResource from "./AuthResource";

export default class ActionDetailsResource extends AuthResource {
    readonly submissionId: number = -1;
    readonly submittedAt: string = ""; //comes in format yyyy-mm-ddThh:mm:ss.ssszzzz
    readonly submitter: string | undefined = undefined;
    readonly httpStatus: number = -1;
    readonly externalName: string = "";
    readonly id: string = "";
    readonly destinations: Destination[] = [];
    readonly errors: ReportError[] = [];
    readonly warnings: ReportWarning[] = [];
    readonly topic: string = "";
    readonly warningCount: number = -1;
    readonly errorCount: number = -1;

    pk() {
        return `${this.submissionId}-${this.submitter}`;
    }

    /* 
       Since we won't be using urlRoot to build our urls we still need to tell rest hooks
       how to uniquely identify this Resource
    */
    static get key() {
        return "ActionDetailsResource";
    }

    static url(searchParams: {
        actionId: string;
        organization: string;
    }): string {
        if (searchParams && Object.keys(searchParams).length) {
            return `${process.env.REACT_APP_BACKEND_URL}/api/history/${searchParams.organization}/submissions/${searchParams.actionId}`;
        }
        throw new Error("Action details require an ActionID to retrieve");
    }

    static listDummy(many: number): ActionDetailsResource[] {
        let list: ActionDetailsResource[] = [];
        for (let i in list) {
            list[i] = ActionDetailsResource.dummy();
        }
        return list;
    }

    static dummy(): ActionDetailsResource {
        /* 
            The rest of this will be pertinent to fill in later, but is okay
            to leave blank until we need it. 
        */
        return {
            submissionId: 12345,
            submittedAt: "1970-04-07T16:26:14.34593Z",
            submitter: "Jest",
            httpStatus: 201,
            externalName: "SubmissionDetails Unit Test",
            id: "f4072cc6-7ea0-4070-8e60-34a584b7c822",
            destinations: [
                {
                    organization_id: "jest",
                    organization: "React Unit Testing",
                    service: "QUALITY_PASS",
                    filteredReportRows: [
                        "For ignore.QUALITY_PASS, filter matches[ordering_facility_county, QUALITY_PASS] filtered out item 682740 at index 1",
                        "For ignore.QUALITY_PASS, filter matches[ordering_facility_county, QUALITY_PASS] filtered out item 496898 at index 3",
                    ],
                    sending_at: "1970-04-07T16:26:14.34593Z",
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
}
