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
    errorCode: ErrorCode;
    trackingIds: string[] | undefined;
    details: any | undefined;
    rowList?: string;
}

export enum WatersUrls {
    VALIDATE = "/validate",
}

/*
The error codes map to the error types specified in the serializer
 */
export enum ErrorCode {
    INVALID_MSG_PARSE_BLANK = "INVALID_MSG_PARSE_BLANK",
    INVALID_HL7_MSG_TYPE_MISSING = "INVALID_HL7_MSG_TYPE_MISSING",
    INVALID_HL7_MSG_TYPE_UNSUPPORTED = "INVALID_HL7_MSG_TYPE_UNSUPPORTED",
    INVALID_HL7_MSG_FORMAT_INVALID = "INVALID_HL7_MSG_FORMAT_INVALID",
    INVALID_MSG_PARSE_DATETIME = "INVALID_MSG_PARSE_DATETIME",
    INVALID_MSG_PARSE_TELEPHONE = "INVALID_MSG_PARSE_TELEPHONE",
    INVALID_MSG_PARSE_DATE = "INVALID_MSG_PARSE_DATE",
    INVALID_HL7_MSG_VALIDATION = "INVALID_HL7_MSG_VALIDATION",
    INVALID_MSG_MISSING_FIELD = "INVALID_MSG_MISSING_FIELD",
    INVALID_MSG_EQUIPMENT_MAPPING = "INVALID_MSG_EQUIPMENT_MAPPING",
    UNKNOWN = "UNKNOWN",
}

/*
Waters Endpoints
* waters -> uploads a file to the ReportStream service
* validate -> validates a file against ReportStream file requirements (filters, data quality, etc.)
*/
export const watersEndpoints: RSApiEndpoints = {
    validate: new RSEndpoint({
        path: WatersUrls.VALIDATE,
        method: HTTPMethods.POST,
        queryKey: "watersValidate",
    }),
};
