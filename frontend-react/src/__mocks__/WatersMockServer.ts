import { rest } from "msw";
import { setupServer } from "msw/node";

import { WatersResponse } from "../network/api/WatersApi";

const watersResponseSuccess: WatersResponse = {
    id: "uuid-string",
    submissionId: 650,
    overallStatus: "Waiting to Deliver",
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

const handlers = [
    rest.post(
        `${process.env.REACT_APP_BACKEND_URL}/api/waters`,
        (req, res, ctx) => {
            if (req.headers["_headers"]["client"] === "bad-client") {
                return res(ctx.json(watersResponseError), ctx.status(400));
            }

            if (
                req.headers["_headers"]["client"] ===
                "give me a very bad response"
            ) {
                return res(
                    ctx.text(
                        "This response will not parse and will cause an error"
                    ),
                    ctx.status(500)
                );
            }

            return res(ctx.json(watersResponseSuccess), ctx.status(201));
        }
    ),
];

export const watersServer = setupServer(...handlers);
