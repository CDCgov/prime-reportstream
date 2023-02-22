import { rest } from "msw";
import { setupServer } from "msw/node";

import { WatersUrls } from "../config/endpoints/waters";
import config from "../config";

const { RS_API_URL } = config;

export enum WatersTestHeader {
    CLIENT = "client",
}
export enum WatersTestHeaderValue {
    TEST_BAD_CLIENT = "bad-client",
    TEST_NAME = "test-endpoint-name",
    FAIL = "test-fail",
}

const handlers = [
    rest.post(`${RS_API_URL}/api${WatersUrls.VALIDATE}`, (req, res, ctx) => {
        if (
            req.headers.get(WatersTestHeader.CLIENT) ===
            WatersTestHeaderValue.FAIL
        )
            return res(ctx.status(400));
        if (
            req.headers.get(WatersTestHeader.CLIENT) ===
            WatersTestHeaderValue.TEST_NAME
        ) {
            return res(ctx.status(201), ctx.json({ endpoint: "validate" }));
        }
        return res(ctx.status(200));
    }),
];

export const watersServer = setupServer(...handlers);
