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
}
