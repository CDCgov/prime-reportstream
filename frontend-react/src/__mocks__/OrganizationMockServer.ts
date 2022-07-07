import { rest } from "msw";
import { setupServer } from "msw/node";

const base = "https://test.prime.cdc.gov/api/settings/organizations";
const getSender = (org: string, sender: string) =>
    `${base}/${org}/senders/${sender}`;
const testSender = getSender("testOrg", "testSender");
const firstSender = getSender("firstOrg", "firstSender");

export const dummySender = {
    name: "testSender",
    organizationName: "testOrg",
    format: "CSV",
    topic: "covid-19",
    customerStatus: "testing",
    schemaName: "test/covid-19-test",
};

const fakeOrg = {
    countyName: "Testing County",
    description: "A county for testing",
    filters: [],
    jurisdiction: "TC",
    version: 1,
    createdBy: "OrganizationMockServer",
    createdAt: "now",
    name: "Fake Org",
    stateCode: "TC",
};

const handlers = [
    rest.get(base, (_req, res, ctx) => {
        return res(
            ctx.json([fakeOrg, fakeOrg, fakeOrg, fakeOrg]),
            ctx.status(200)
        );
    }),
    rest.get(testSender, (req, res, ctx) => {
        return res(ctx.json(dummySender), ctx.status(200));
    }),
    rest.get(firstSender, (req, res, ctx) => {
        return res(ctx.status(200));
    }),
];

export const orgServer = setupServer(...handlers);
