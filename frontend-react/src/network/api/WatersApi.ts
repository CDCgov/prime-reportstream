import { Destination } from "../../resources/ActionDetailsResource";

import { Api, EndpointConfig } from "./Api";

export interface WatersResponse {
    actualCompletionAt: string | null;
    destinationCount: number;
    destinations: Destination[];
    errorCount: number;
    errors: ResponseError[];
    externalName: string | undefined;
    httpStatus: number;
    id: string | undefined;
    overallStatus: string;
    plannedCompletionAt: string | null;
    reportItemCount: number;
    sender: string | null;
    submissionId: number;
    timestamp: string;
    topic: string | null;
    warningCount: number;
    warnings: ResponseError[];
    ok: boolean;
}

export interface ResponseError {
    field: string;
    indices: number[];
    message: string;
    scope: string;
    trackingIds: string[];
    details: any;
}

class WatersApi extends Api {
    postReport = (
        client: string,
        fileName: string,
        contentType: string
    ): EndpointConfig<WatersResponse> => {
        return this.configure<WatersResponse>({
            method: "POST",
            url: `${this.basePath}`,
            headers: {
                ...this.headers,
                "authentication-type": "okta",
                payloadName: fileName,
                client,
                "Content-Type": contentType,
            },
        });
    };
}

export const watersApi = new WatersApi("waters");
