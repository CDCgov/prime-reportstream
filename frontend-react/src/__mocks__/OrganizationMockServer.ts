import { rest } from "msw";
import { setupServer } from "msw/node";

import { dummySender } from "../hooks/UseSenderMode.test";
import { Organization, orgApi } from "../network/api/OrgApi";

const orgList = orgApi.getOrgList();
const firstSender = orgApi.getSenderDetail("firstOrg", "firstSender");
const testSender = orgApi.getSenderDetail("testOrg", "testSender");

const fakeOrg: Organization = {
    countyName: "Testing County",
    description: "A county for testing",
    filters: [],
    jurisdiction: "TC",
    meta: {
        version: 1,
        createdBy: "OrganizationMockServer",
        createdAt: "now",
    },
    name: "Fake Org",
    stateCode: "TC",
};

const handlers = [
    rest.get(orgList.url, (_req, res, ctx) => {
        return res(
            ctx.json([fakeOrg, fakeOrg, fakeOrg, fakeOrg]),
            ctx.status(200)
        );
    }),
    rest.get(testSender.url, (req, res, ctx) => {
        return res(ctx.json(dummySender), ctx.status(200));
    }),
    rest.get(firstSender.url, (req, res, ctx) => {
        return res(ctx.status(200));
    }),
];

export const orgServer = setupServer(...handlers);
