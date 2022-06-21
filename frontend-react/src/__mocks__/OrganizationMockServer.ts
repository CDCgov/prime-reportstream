import { rest } from "msw";
import { setupServer } from "msw/node";

// import { dummySender } from "../hooks/UseSenderMode.test";
import { Organization, orgApi, RSSender } from "../network/api/OrgApi";

const orgList = orgApi.getOrgList();
const firstSender = orgApi.getSenderDetail("firstOrg", "firstSender");

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

export const testSender = new RSSender(
    "testSender",
    "testOrg",
    "CSV",
    "covid-19",
    "testing",
    "test/covid-19-test"
);

const handlers = [
    rest.get(orgList.url, (_req, res, ctx) => {
        return res(
            ctx.json([fakeOrg, fakeOrg, fakeOrg, fakeOrg]),
            ctx.status(200)
        );
    }),
    rest.get(
        "https://test.prime.cdc.gov/api/settings/organizations/testOrg/sender/testSender",
        (req, res, ctx) => {
            return res(ctx.json(testSender), ctx.status(200));
        }
    ),
    rest.get(firstSender.url, (req, res, ctx) => {
        return res(ctx.status(200));
    }),
];

export const orgServer = setupServer(...handlers);
