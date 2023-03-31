/*
  below is an example of a mocked File & mocked React file input change event we can use for future tests
  thx to https://evanteague.medium.com/creating-fake-test-events-with-typescript-jest-778018379d1e
*/
import {
    ErrorCode,
    OverallStatus,
    WatersResponse,
} from "../config/endpoints/waters";

export const contentString = "foo,bar\r\nbar,foo";
// doesn't work out of the box as it somehow doesn't come with a .text method
export const fakeFile = new File([new Blob([contentString])], "file.csv", {
    type: "text/csv",
});
fakeFile.text = () => Promise.resolve(contentString);

export const mockSendValidFile: WatersResponse = {
    id: "",
    submissionId: 1,
    overallStatus: OverallStatus.VALID,
    sender: "",
    errorCount: 0,
    warningCount: 0,
    httpStatus: 200,
    errors: [],
    warnings: [],
};

export const mockSendFileWithWarnings: WatersResponse = {
    id: "",
    submissionId: 1,
    overallStatus: OverallStatus.VALID,
    sender: "",
    errorCount: 0,
    warningCount: 2,
    httpStatus: 200,
    errors: [],
    warnings: [
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "MSH-7 (file_created_date)",
            message:
                "Timestamp for file_created_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "ORC-15 (order_test_date)",
            message:
                "Timestamp for order_test_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
    ],
};

export const mockSendFileWithErrors: WatersResponse = {
    id: "",
    submissionId: 1,
    overallStatus: OverallStatus.ERROR,
    sender: "",
    errorCount: 2,
    warningCount: 0,
    httpStatus: 200,
    errors: [
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "MSH-7 (file_created_date)",
            message:
                "Timestamp for file_created_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "ORC-15 (order_test_date)",
            message:
                "Timestamp for order_test_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
    ],
    warnings: [],
};
