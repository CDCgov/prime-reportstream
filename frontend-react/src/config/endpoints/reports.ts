import { HTTPMethods, type RSApiEndpoints, RSEndpoint } from ".";

export enum ReportsUrls {
    TESTING = "/reports/testing",
}

export interface RSMessage {
    dateCreated?: string;
    fileName: string;
    reportBody: string;
}

export interface RSMessageResult {
    message?: string;
    bundle?: string;
    senderTransformPassed: boolean;
    senderTransformErrors: string[];
    senderTransformWarnings: string[];
    enrichmentSchemaPassed: boolean;
    enrichmentSchemaErrors: string[];
    enrichmentSchemaWarnings: string[];
    receiverTransformPassed: boolean;
    receiverTransformErrors: string[];
    receiverTransformWarnings: string[];
    filterErrors: string[];
    filtersPassed: boolean;
}

export const reportsEndpoints: RSApiEndpoints = {
    testing: new RSEndpoint({
        path: ReportsUrls.TESTING,
        method: HTTPMethods.GET,
        queryKey: "reportsTesting",
    }),
};
