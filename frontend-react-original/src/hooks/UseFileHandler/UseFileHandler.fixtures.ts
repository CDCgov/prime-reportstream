import { ErrorCode, ResponseError } from "../../config/endpoints/waters";

export const fakeError: ResponseError = {
    field: "error field",
    indices: [1],
    message: "error message",
    trackingIds: ["track me"],
    scope: "some scope",
    errorCode: ErrorCode.INVALID_HL7_MSG_VALIDATION,
    details: "this happened",
};

export const fakeWarning: ResponseError = {
    field: "warning field",
    indices: [1],
    message: "warning message",
    trackingIds: ["track me"],
    scope: "some warning scope",
    errorCode: ErrorCode.INVALID_HL7_MSG_VALIDATION,
    details: "this happened - a warning",
};
