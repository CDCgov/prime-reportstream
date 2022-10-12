import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    WatersResponse,
    OverallStatus,
    WatersUrls,
} from "../config/endpoints/waters";
import config from "../config";

const { RS_API_URL } = config;

const watersResponseSuccess: WatersResponse = {
    id: "uuid-string",
    submissionId: 650,
    overallStatus: OverallStatus.WAITING_TO_DELIVER,
    timestamp: "2022-06-14T18:57:36.941Z",
    plannedCompletionAt: "2022-06-14T18:58:00.000Z",
    actualCompletionAt: "",
    sender: "test.default",
    reportItemCount: 2,
    errorCount: 0,
    warningCount: 0,
    httpStatus: 201,
    destinations: [
        {
            filteredReportRows: [],
            itemCount: 1,
            organization: "New York Public Health Department",
            organization_id: "ny-phd",
            sending_at: "2022-06-14T18:58:00.000Z",
            sentReports: [],
            service: "elr",
            filteredReportItems: [],
            itemCountBeforeQualityFiltering: 0,
        },
    ],
    errors: [],
    warnings: [],
    topic: "covid-19",
    externalName: "test-file.hl7",
    destinationCount: 1,
    ok: true,
};

const watersResponseError = {
    id: null,
    submissionId: 651,
    overallStatus: "Error",
    timestamp: "2022-06-14T18:57:36.941Z",
    plannedCompletionAt: null,
    actualCompletionAt: "",
    sender: null,
    reportItemCount: null,
    errorCount: 1,
    warningCount: 0,
    httpStatus: 400,
    destinations: [],
    errors: [
        {
            field: "",
            indices: [1],
            message:
                "The GMT offset hour value of the TM datatype must be >=0 and <=23",
            scope: "item",
            trackingIds: ["message1"],
        },
    ],
    warnings: [],
    topic: null,
    externalName: null,
    destinationCount: null,
};

export enum WatersTestHeader {
    CLIENT = "client",
}
export enum WatersTestHeaderValue {
    TEST_BAD_CLIENT = "bad-client",
    TEST_NAME = "test-endpoint-name",
    FAIL = "test-fail",
}

const handlers = [
    rest.post(`${RS_API_URL}/api${WatersUrls.UPLOAD}`, (req, res, ctx) => {
        if (
            req.headers["_headers"][WatersTestHeader.CLIENT] ===
            WatersTestHeaderValue.TEST_NAME
        )
            return res(ctx.status(200), ctx.json({ endpoint: "upload" }));
        if (
            req.headers["_headers"][WatersTestHeader.CLIENT] ===
            WatersTestHeaderValue.TEST_BAD_CLIENT
        ) {
            return res(ctx.json(watersResponseError), ctx.status(400));
        }
        return res(ctx.json(watersResponseSuccess), ctx.status(201));
    }),
    rest.post(`${RS_API_URL}/api${WatersUrls.VALIDATE}`, (req, res, ctx) => {
        if (
            req.headers["_headers"][WatersTestHeader.CLIENT] ===
            WatersTestHeaderValue.FAIL
        )
            return res(ctx.status(400));
        if (
            req.headers["_headers"][WatersTestHeader.CLIENT] ===
            WatersTestHeaderValue.TEST_NAME
        ) {
            return res(ctx.status(201), ctx.json({ endpoint: "validate" }));
        }
        return res(ctx.status(200));
    }),
];

export const watersServer = setupServer(...handlers);
