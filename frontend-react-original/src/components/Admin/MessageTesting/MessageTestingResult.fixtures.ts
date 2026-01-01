import type { RSFilterError, RSMessageResult } from "../../../config/endpoints/reports";

const sampleFilterError = {
    filter: "Sample Filter",
    filterType: "QUALITY_FILTER",
    message: "Sample message, sample text.",
} as RSFilterError;

export const passMessageResult: RSMessageResult = {
    senderTransformPassed: true,
    senderTransformErrors: [],
    senderTransformWarnings: [],
    enrichmentSchemaPassed: true,
    enrichmentSchemaErrors: [],
    enrichmentSchemaWarnings: [],
    receiverTransformPassed: true,
    receiverTransformErrors: [],
    receiverTransformWarnings: [],
    filterErrors: [],
    filtersPassed: true,
    message: "pass output message",
};

export const errorMessageResult: RSMessageResult = {
    senderTransformPassed: false,
    senderTransformErrors: ["senderTransformError 1", "senderTransformError 2"],
    senderTransformWarnings: [],
    enrichmentSchemaPassed: false,
    enrichmentSchemaErrors: ["enrichmentSchemaError 1", "enrichmentSchemaError 2"],
    enrichmentSchemaWarnings: [],
    receiverTransformPassed: false,
    receiverTransformErrors: ["receiverTransformError 1", "receiverTransformError 2"],
    receiverTransformWarnings: [],
    filterErrors: [sampleFilterError],
    filtersPassed: false,
};

export const warningMessageResult: RSMessageResult = {
    senderTransformPassed: true,
    senderTransformErrors: [],
    senderTransformWarnings: ["senderTransformWarning 1", "senderTransformWarning 2"],
    enrichmentSchemaPassed: true,
    enrichmentSchemaErrors: [],
    enrichmentSchemaWarnings: ["enrichmentSchemaWarning 1", "enrichmentSchemaWarning 2"],
    receiverTransformPassed: true,
    receiverTransformErrors: [],
    receiverTransformWarnings: ["receiverTransformWarning 1", "receiverTransformWarning 2"],
    filterErrors: [],
    filtersPassed: true,
    message: "warning output message",
};
