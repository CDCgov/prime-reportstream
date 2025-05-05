export const MOCK_GET_MESSAGES = [
    {
        id: 0,
        messageId: "582098",
        sender: "simple_report.default",
        submittedDate: "2024-05-14T14:26:55.510171",
        reportId: "73e3cbc8-9920-4ab7-871f-843a1db4c074",
        fileName: null,
        fileUrl: null,
        warnings: [],
        errors: [],
        receiverData: [],
    },
    {
        id: 1,
        messageId: "582098",
        sender: "simple_report.default",
        submittedDate: "2022-01-27T19:05:33.393786",
        reportId: "158285f7-2aeb-4e1b-893c-394335f9ed42",
        fileName: null,
        fileUrl: null,
        warnings: [],
        errors: [],
        receiverData: [],
    },
];

export const MOCK_GET_MESSAGE = {
    id: 0,
    messageId: "582098",
    sender: "simple_report.default",
    submittedDate: "2024-05-14T14:26:55.510171",
    reportId: "d9a57df0-2702-4e28-9d80-ff8c9ec51816",
    fileName: null,
    fileUrl:
        "https://pdhstagingstorageaccount.blob.core.windows.net/reports/receive%2Fignore.ignore-simple-report%2Fpdi-covid-19-d9a57df0-2702-4e28-9d80-ff8c9ec51816-20240514142655.csv",
    warnings: [],
    errors: [],
    receiverData: [
        {
            reportId: "1bf1224e-1c9d-46a8-8eed-81e5acb47ea0",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7_BATCH",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/batch%2Fignore.HL7_BATCH%2Faz-covid-19-hl7-1bf1224e-1c9d-46a8-8eed-81e5acb47ea0-20240514142656.internal.csv",
            createdAt: "2024-05-14T14:26:56.947681",
            qualityFilters: [],
        },
        {
            reportId: "24ffa08e-9cf6-441f-bc2c-fc1ce659f17e",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.HL7%2Ffl-covid-19-24ffa08e-9cf6-441f-bc2c-fc1ce659f17e-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
        {
            reportId: "2da499a0-9efb-4008-aabe-196e0eb9ce43",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult:
                "Success: sftp upload of fl-covid-19-507e8a18-474e-4574-8b6b-e4f043a098a0-20240514142709.hl7 to SFTPTransportType(host=172.17.6.20, port=22, filePath=./upload, credentialName=null)",
            fileName: "fl-covid-19-507e8a18-474e-4574-8b6b-e4f043a098a0-20240514142709.hl7",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.HL7%2Ffl-covid-19-507e8a18-474e-4574-8b6b-e4f043a098a0-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:16.057366",
            qualityFilters: [],
        },
        {
            reportId: "3c76ef13-bf3f-48b8-b268-ffebfda9bed0",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult:
                "Success: sftp upload of fl-covid-19-24ffa08e-9cf6-441f-bc2c-fc1ce659f17e-20240514142709.hl7 to SFTPTransportType(host=172.17.6.20, port=22, filePath=./upload, credentialName=null)",
            fileName: "fl-covid-19-24ffa08e-9cf6-441f-bc2c-fc1ce659f17e-20240514142709.hl7",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.HL7%2Ffl-covid-19-24ffa08e-9cf6-441f-bc2c-fc1ce659f17e-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:16.182365",
            qualityFilters: [],
        },
        {
            reportId: "46d3b465-4df1-4cd2-9294-15b11cd52c80",
            receivingOrg: "ignore",
            receivingOrgSvc: "CSV",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/batch%2Fignore.CSV%2Fpima-az-covid-19-46d3b465-4df1-4cd2-9294-15b11cd52c80-20240514142656.internal.csv",
            createdAt: "2024-05-14T14:26:56.947681",
            qualityFilters: [],
        },
    ],
};
