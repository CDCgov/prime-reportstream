import { HTTPMethods, type RSApiEndpoints, RSEndpoint } from ".";

export enum ReportsUrls {
    TESTING = "/reports/testing",
    TEST_RESULT = "/reports/testing/test",
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
    test: new RSEndpoint({
        path: ReportsUrls.TESTING,
        method: HTTPMethods.GET,
        queryKey: "reportsTest",
    }),
    testResult: new RSEndpoint({
        path: ReportsUrls.TEST_RESULT,
        method: HTTPMethods.POST,
        queryKey: "reportsTestResult",
    }),
};
