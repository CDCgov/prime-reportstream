import { rest } from "msw";
import { setupServer } from "msw/node";

import { HistoryApi } from "../network/api/History";
import { Endpoint } from "../network/api/Api";

/* TEST ENDPOINTS */
const listEndpoint: Endpoint = HistoryApi.list();
const detailSuccess: Endpoint = HistoryApi.detail("test");
const detailFail: Endpoint = HistoryApi.detail("fail");

const handlers = [
    /* Successfully returns array of Reports */
    rest.get(`http://localhost:3000${listEndpoint.url}`, (req, res, ctx) => {
        return res(ctx.json(HistoryApi.testResponse(3)));
    }),

    /* Fails to return array of Reports with 404 */
    rest.get(`http://localhost:3000${detailFail.url}/fail`, (req, res, ctx) => {
        return res(ctx.status(404));
    }),

    /* Successfully returns detailed Report */
    rest.get(`http://localhost:3000${detailSuccess.url}`, (req, res, ctx) => {
        return res(ctx.json(HistoryApi.testResponse(1)));
    }),

    /* Fails to return detailed Report with 404 */
    rest.get(`http://localhost:3000${detailFail.url}`, (req, res, ctx) => {
        return res(ctx.status(404));
    }),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
export const server = setupServer(...handlers);
