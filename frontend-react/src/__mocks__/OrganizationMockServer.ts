import { rest } from "msw";
import { setupServer } from "msw/node";

import config from "../config";
import {
    ApiKey,
    ApiKeySet,
    RSApiKeysResponse,
    RSReceiver,
    RSSender,
} from "../config/endpoints/settings";

const base = `${config.API_ROOT}/settings/organizations`;
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
    allowDuplicates: false,
    processingType: "sync",
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

/** TEST UTILITY - generates `RSSenders[]`, each with a unique `name` (starting from "elr-0")
 *
 * @param count {number} How many unique senders you want. */
export const sendersGenerator = (count: number) => {
    const senders: RSSender[] = [];
    for (let i = 0; i < count; i++) {
        senders.push({
            name: `elr-${i}`,
            organizationName: "testOrg",
            format: "CSV",
            topic: "covid-19",
            customerStatus: "testing",
            schemaName: "test/covid-19-test",
            allowDuplicates: false,
            processingType: "sync",
        });
    }
    return senders;
};

export const dummySenders = sendersGenerator(5);

/** TEST UTILITY - generates `RSReceiver[]`, each with a unique `name` (starting from "elr-0")
 *
 * @param count {number} How many unique receiverServices you want. */
export const receiversGenerator = (count: number) => {
    const receiverServices: RSReceiver[] = [];
    for (let i = 0; i < count; i++) {
        receiverServices.push({
            name: `elr-${i}`,
            organizationName: "testOrg",
        });
    }
    return receiverServices;
};

export const dummyReceivers = receiversGenerator(5);
export const dummyActiveReceiver = dummyReceivers[0];

export const publicKeysGenerator = (apiKeyCount: number) => {
    let publicKey: RSApiKeysResponse;
    const apiKey: ApiKey[] = [];
    const apiKeySet: ApiKeySet[] = [];

    for (let j = 0; j < apiKeyCount; j++) {
        apiKey.push({
            kty: "RSA",
            kid: `testOrg.elr-${j}`,
            n: "asdfaasd",
            e: "AQAB",
        });
    }

    apiKeySet.push({
        scope: `testOrg.*.report`,
        keys: apiKey,
    });

    publicKey = {
        orgName: "testOrg",
        keys: apiKeySet,
    };
    return publicKey;
};

export const dummyPublicKey = publicKeysGenerator(2);

const handlers = [
    rest.get(base, (_req, res, ctx) => {
        return res(
            ctx.json([fakeOrg, fakeOrg, fakeOrg, fakeOrg]),
            ctx.status(200),
        );
    }),
    rest.get(testSender, (req, res, ctx) => {
        return res(ctx.json(dummySender), ctx.status(200));
    }),
    rest.get(`${base}/testOrg/senders`, (req, res, ctx) => {
        return res(ctx.json(dummySenders), ctx.status(200));
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
    rest.get(`${base}/testOrg/public-keys`, (req, res, ctx) => {
        return res(ctx.json(dummyPublicKey), ctx.status(200));
    }),
    rest.post(`${base}/testOrg/public-keys`, (req, res, ctx) => {
        if (!req.headers.get("authorization")?.includes("TOKEN")) {
            return res(ctx.status(401));
        }
        return res(ctx.json(dummyPublicKey), ctx.status(200));
    }),
];

export const orgServer = setupServer(...handlers);
