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
    id: 13569723,
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
            fileName:
                "fl-covid-19-507e8a18-474e-4574-8b6b-e4f043a098a0-20240514142709.hl7",
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
            fileName:
                "fl-covid-19-24ffa08e-9cf6-441f-bc2c-fc1ce659f17e-20240514142709.hl7",
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
        {
            reportId: "4c11a1f9-fe11-4a5f-9825-e8369b55dac7",
            receivingOrg: "ignore",
            receivingOrgSvc: "CSV",
            transportResult:
                "Success: sftp upload of pima-az-covid-19-f0afff43-e75c-4f77-b3ea-0a9da19caca7-20240514142709.csv to SFTPTransportType(host=172.17.6.20, port=22, filePath=./upload, credentialName=null)",
            fileName:
                "pima-az-covid-19-f0afff43-e75c-4f77-b3ea-0a9da19caca7-20240514142709.csv",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.CSV%2Fpima-az-covid-19-f0afff43-e75c-4f77-b3ea-0a9da19caca7-20240514142709.csv",
            createdAt: "2024-05-14T14:27:15.963656",
            qualityFilters: [],
        },
        {
            reportId: "507e8a18-474e-4574-8b6b-e4f043a098a0",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.HL7%2Ffl-covid-19-507e8a18-474e-4574-8b6b-e4f043a098a0-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
        {
            reportId: "5cb16c92-9fa5-428b-93db-ec907be3e198",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/batch%2Fignore.HL7%2Ffl-covid-19-5cb16c92-9fa5-428b-93db-ec907be3e198-20240514142656.internal.csv",
            createdAt: "2024-05-14T14:26:56.947681",
            qualityFilters: [],
        },
        {
            reportId: "5e8f4aab-2c2b-40fe-8a35-6a6ae27e4d2d",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7_BATCH",
            transportResult:
                "Success: sftp upload of az-covid-19-hl7-68c88db1-a697-41d1-b942-ea61b4ef8544-20240514142709.hl7 to SFTPTransportType(host=172.17.6.20, port=22, filePath=./upload, credentialName=null)",
            fileName:
                "az-covid-19-hl7-68c88db1-a697-41d1-b942-ea61b4ef8544-20240514142709.hl7",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.HL7_BATCH%2Faz-covid-19-hl7-68c88db1-a697-41d1-b942-ea61b4ef8544-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:15.901051",
            qualityFilters: [],
        },
        {
            reportId: "68c88db1-a697-41d1-b942-ea61b4ef8544",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7_BATCH",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.HL7_BATCH%2Faz-covid-19-hl7-68c88db1-a697-41d1-b942-ea61b4ef8544-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
        {
            reportId: "6e9a21b0-5854-4262-931f-756ab5703490",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult:
                "Success: sftp upload of fl-covid-19-ce561170-6de3-4040-92d0-49c5c39c447c-20240514142709.hl7 to SFTPTransportType(host=172.17.6.20, port=22, filePath=./upload, credentialName=null)",
            fileName:
                "fl-covid-19-ce561170-6de3-4040-92d0-49c5c39c447c-20240514142709.hl7",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.HL7%2Ffl-covid-19-ce561170-6de3-4040-92d0-49c5c39c447c-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:15.916679",
            qualityFilters: [],
        },
        {
            reportId: "812677b4-a0e7-42c5-b1f1-88b89f837741",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.HL7%2Ffl-covid-19-812677b4-a0e7-42c5-b1f1-88b89f837741-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
        {
            reportId: "a3fc56ed-4dd1-4949-aaf5-3e70327157db",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.HL7%2Ffl-covid-19-a3fc56ed-4dd1-4949-aaf5-3e70327157db-20240514142710.hl7",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
        {
            reportId: "a79aa931-b101-4c06-8a92-33435f295736",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7_NULL",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.HL7_NULL%2Ftx-covid-19-a79aa931-b101-4c06-8a92-33435f295736-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
        {
            reportId: "a920ae7f-c2b4-4c71-85e5-0eb9fb4b73fb",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7_NULL",
            transportResult: "Sending to Null Transport",
            fileName:
                "tx-covid-19-a79aa931-b101-4c06-8a92-33435f295736-20240514142709.hl7",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.HL7_NULL%2Ftx-covid-19-a79aa931-b101-4c06-8a92-33435f295736-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:15.010468",
            qualityFilters: [],
        },
        {
            reportId: "c30abcdc-a2ef-4d7d-a6b2-8ce225844d1f",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7_NULL",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/batch%2Fignore.HL7_NULL%2Ftx-covid-19-c30abcdc-a2ef-4d7d-a6b2-8ce225844d1f-20240514142656.internal.csv",
            createdAt: "2024-05-14T14:26:56.947681",
            qualityFilters: [],
        },
        {
            reportId: "c8f6ba4f-da0c-494a-8f77-8f55823000e6",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult:
                "Success: sftp upload of fl-covid-19-a3fc56ed-4dd1-4949-aaf5-3e70327157db-20240514142710.hl7 to SFTPTransportType(host=172.17.6.20, port=22, filePath=./upload, credentialName=null)",
            fileName:
                "fl-covid-19-a3fc56ed-4dd1-4949-aaf5-3e70327157db-20240514142710.hl7",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.HL7%2Ffl-covid-19-a3fc56ed-4dd1-4949-aaf5-3e70327157db-20240514142710.hl7",
            createdAt: "2024-05-14T14:27:16.19799",
            qualityFilters: [],
        },
        {
            reportId: "ce561170-6de3-4040-92d0-49c5c39c447c",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.HL7%2Ffl-covid-19-ce561170-6de3-4040-92d0-49c5c39c447c-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
        {
            reportId: "dcd66c35-dc89-4677-83f5-f50ecbbdfa8e",
            receivingOrg: "dataingestion",
            receivingOrgSvc: "datateam-cdc-nbs",
            transportResult: null,
            fileName: null,
            fileUrl: null,
            createdAt: "2024-05-14T14:26:56.947681",
            qualityFilters: [],
        },
        {
            reportId: "e46a3954-11e0-4010-8faf-5ead8a9d8fbb",
            receivingOrg: "ignore",
            receivingOrgSvc: "HL7",
            transportResult:
                "Success: sftp upload of fl-covid-19-812677b4-a0e7-42c5-b1f1-88b89f837741-20240514142709.hl7 to SFTPTransportType(host=172.17.6.20, port=22, filePath=./upload, credentialName=null)",
            fileName:
                "fl-covid-19-812677b4-a0e7-42c5-b1f1-88b89f837741-20240514142709.hl7",
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/none%2Fignore.HL7%2Ffl-covid-19-812677b4-a0e7-42c5-b1f1-88b89f837741-20240514142709.hl7",
            createdAt: "2024-05-14T14:27:15.916679",
            qualityFilters: [],
        },
        {
            reportId: "f0afff43-e75c-4f77-b3ea-0a9da19caca7",
            receivingOrg: "ignore",
            receivingOrgSvc: "CSV",
            transportResult: null,
            fileName: null,
            fileUrl:
                "https://pdhstagingstorageaccount.blob.core.windows.net/reports/ready%2Fignore.CSV%2Fpima-az-covid-19-f0afff43-e75c-4f77-b3ea-0a9da19caca7-20240514142709.csv",
            createdAt: "2024-05-14T14:27:09.776012",
            qualityFilters: [],
        },
    ],
};
