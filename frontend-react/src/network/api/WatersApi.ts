import { Destination } from "../../resources/ActionDetailsResource";

import { API } from "./NewApi";

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
    overallStatus?: string;
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
    trackingIds: string[] | undefined;
    details: any | undefined;
    rowList?: string;
}

export enum EndpointName {
    WATERS = "waters",
    VALIDATE = "validate",
}
const WatersApi: API = new API(WatersResponse, "/api")
    .addEndpoint(EndpointName.WATERS.toString(), "/waters", ["POST"])
    .addEndpoint(EndpointName.VALIDATE.toString(), "/validate", ["POST"]);

export default WatersApi;
