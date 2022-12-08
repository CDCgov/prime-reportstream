import { rest } from "msw";
import { setupServer } from "msw/node";

import { RSReceiver } from "../config/endpoints/settings";

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

export const fakeOrg = {
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

/** TEST UTILITY - generates `RSReceiver[]`, each with a unique `name` (starting from "elr-0")
 *
 * @param count {number} How many unique receivers you want. */
export const receiversGenerator = (count: number) => {
    const receivers: RSReceiver[] = [];
    for (let i = 0; i < count; i++) {
        receivers.push({ name: `elr-${i}`, organizationName: "testOrg" });
    }
    return receivers;
};

export const dummyReceivers = receiversGenerator(5);
export const dummyActiveReceiver = dummyReceivers[0];

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
    rest.get(`${base}/testOrg`, (req, res, ctx) => {
        return res(ctx.json(fakeOrg), ctx.status(200));
    }),
    rest.get(`${base}/testOrg/receivers`, (req, res, ctx) => {
        return res(ctx.json(dummyReceivers), ctx.status(200));
    }),
    rest.get(`${base}/testOrgNoReceivers/receivers`, (req, res, ctx) => {
        return res(ctx.json([]), ctx.status(200));
    }),
];

export const orgServer = setupServer(...handlers);
