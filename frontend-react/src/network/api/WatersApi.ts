import { Destination } from "../../resources/ActionDetailsResource";
import {
    HTTPMethods,
    RSApiEndpoints,
    RSEndpoint,
} from "../../config/endpoints";

import { API } from "./NewApi";

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

/*
Waters Endpoints

* waters -> returns metadata for all lookuptables
* validate -> given a table name, returns all data rows in the lookup table of that name, for the active version
*/

export const watersEndpoints: RSApiEndpoints = {
    postWaters: new RSEndpoint({
        path: "/waters",
        method: HTTPMethods.POST,
        queryKey: "watersPost",
    }),
    postValidate: new RSEndpoint({
        path: "/validate",
        method: HTTPMethods.POST,
        queryKey: "watersValidate",
    }),
};

export default WatersApi;
