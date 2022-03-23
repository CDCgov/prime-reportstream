import { rest } from "msw";
import { setupServer } from "msw/node";

import { dummySender } from "../hooks/UseSenderMode.test";
import { orgApi } from "../network/api/OrgApi";

const firstSender = orgApi.getSenderDetail("firstOrg", "firstSender");
const testSender = orgApi.getSenderDetail("testOrg", "testSender");

const handlers = [
    rest.get(testSender.url, (req, res, ctx) => {
        return res(ctx.json(dummySender), ctx.status(200));
    }),
    rest.get(firstSender.url, (req, res, ctx) => {
        return res(ctx.status(200));
    }),
];

export const orgServer = setupServer(...handlers);
