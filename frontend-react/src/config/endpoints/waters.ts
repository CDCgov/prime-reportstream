import { Destination } from "../../resources/ActionDetailsResource";

import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export enum OverallStatus {
    VALID = "Valid",
    ERROR = "Error",
    WAITING_TO_DELIVER = "Waiting to Deliver",
}

/*
  shape of response from the Waters API
  @todo refactor to move away from all of these optional fields. Which of these are actually optional?
*/
export class WatersResponse {
    actualCompletionAt?: string;
    destinationCount?: number;
    destinations?: Destination[];
    errorCount?: number;
    errors?: ResponseError[];
    externalName?: string;
    httpStatus?: number;
    id?: string;
    overallStatus?: OverallStatus;
    plannedCompletionAt?: string;
    reportItemCount?: number;
    sender?: string;
    submissionId?: number;
    timestamp?: string;
    topic?: string;
    warningCount?: number;
    warnings?: ResponseError[];
    ok?: boolean;
    status?: number;
}

export interface ResponseError {
    field: string | undefined;
    indices: number[] | undefined;
    message: string | undefined;
    scope: string | undefined;
    errorCode: keyof typeof ErrorCodeTranslation;
    trackingIds: string[] | undefined;
    details: any | undefined;
    rowList?: string;
}

export enum WatersUrls {
    UPLOAD = "/waters",
    VALIDATE = "/validate",
}

/*
The error codes map to the error types specified in the serializer
 */
export enum ErrorCodeTranslation {
    INVALID_HL7_MESSAGE_VALIDATION = "Invalid entry for field.",
    INVALID_HL7_MESSAGE_DATE_VALIDATION = "Invalid entry for field. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
    INVALID_HL7_MESSAGE_FORMAT = "",
    INVALID_HL7_PHONE_NUMBER = "The string supplied is not a valid phone number. Reformat to a 10-digit phone number (e.g. (555) 555-5555).",
}

/*
Waters Endpoints
* waters -> uploads a file to the ReportStream service
* validate -> validates a file against ReportStream file requirements (filters, data quality, etc.)
*/
export const watersEndpoints: RSApiEndpoints = {
    upload: new RSEndpoint({
        path: WatersUrls.UPLOAD,
        method: HTTPMethods.POST,
        queryKey: "watersPost",
    }),
    validate: new RSEndpoint({
        path: WatersUrls.VALIDATE,
        method: HTTPMethods.POST,
        queryKey: "watersValidate",
    }),
};
