import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

import config from "../config";
import { WatersUrls } from "../config/endpoints/waters";

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
    http.post(`${RS_API_URL}/api${WatersUrls.VALIDATE}`, ({ request }) => {
        if (
            request.headers.get(WatersTestHeader.CLIENT) ===
            WatersTestHeaderValue.FAIL
        )
            return HttpResponse.json(null, { status: 400 });
        if (
            request.headers.get(WatersTestHeader.CLIENT) ===
            WatersTestHeaderValue.TEST_NAME
        ) {
            return HttpResponse.json({ endpoint: "validate" }, { status: 201 });
        }
        return HttpResponse.json(null, { status: 200 });
    }),
];

export const watersServer = setupServer(...handlers);
