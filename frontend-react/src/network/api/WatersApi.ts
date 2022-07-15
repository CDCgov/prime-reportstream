import { Destination } from "../../resources/ActionDetailsResource";

import { API } from "./NewApi";

export class WatersResponse {
    actualCompletionAt: string | undefined;
    destinationCount: number | undefined;
    destinations: Destination[] | undefined;
    errorCount: number | undefined;
    errors: ResponseError[] | undefined;
    externalName: string | undefined;
    httpStatus: number | undefined;
    id: string | undefined;
    overallStatus: string | undefined;
    plannedCompletionAt: string | undefined;
    reportItemCount: number | undefined;
    sender: string | undefined;
    submissionId: number | undefined;
    timestamp: string | undefined;
    topic: string | undefined;
    warningCount: number | undefined;
    warnings: ResponseError[] | undefined;
    ok: boolean | undefined;
    status?: number;
}

export interface ResponseError {
    field: string | undefined;
    indices: number[] | undefined;
    message: string | undefined;
    scope: string | undefined;
    trackingIds: string[] | undefined;
    details: any | undefined;
}

export interface FileResponseError extends ResponseError {
    rowList?: string;
}

const WatersApi: API = new API(WatersResponse, "/api").addEndpoint(
    "waters",
    "/waters",
    ["POST"]
);

export default WatersApi;
