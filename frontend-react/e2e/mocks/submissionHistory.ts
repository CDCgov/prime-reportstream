export const MOCK_GET_SUBMISSION_HISTORY = {
    id: "73e3cbc8-9920-4ab7-871f-843a1db4c074",
    submissionId: 3812533,
    overallStatus: "Delivered",
    timestamp: "2024-03-07T18:00:22.636Z",
    plannedCompletionAt: null,
    actualCompletionAt: "2024-03-07T18:01:10.684Z",
    sender: "ignore.ignore-elr-elims",
    reportItemCount: 1,
    errorCount: 0,
    warningCount: 1,
    httpStatus: 201,
    destinations: [
        {
            organization: "FOR TESTING ONLY",
            organization_id: "ignore",
            service: "ELR_ELIMS",
            itemCount: 1,
            itemCountBeforeQualityFiltering: 1,
            filteredReportRows: [],
            filteredReportItems: [],
            sentReports: [
                {
                    externalName:
                        "None-46c8c28d-e6a9-4c8a-9244-7479e05b12ca-20240307180110.hl7",
                    createdAt: "2024-03-07T18:01:10.684Z",
                    itemCount: 1,
                },
            ],
            downloadedReports: [],
        },
    ],
    actionName: "receive",
    externalName: null,
    reportId: "73e3cbc8-9920-4ab7-871f-843a1db4c074",
    topic: "elr-elims",
    errors: [],
    warnings: [
        {
            scope: "report",
            message:
                "Data type error: 'CE' in record 1 is invalid for version 2.7 at OBX-2(0)",
            errorCode: "UNKNOWN",
        },
    ],
    destinationCount: 1,
};
