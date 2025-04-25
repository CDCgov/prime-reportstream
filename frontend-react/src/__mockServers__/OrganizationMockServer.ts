import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

import config from "../config";
import { ApiKey, ApiKeySet, RSApiKeysResponse, RSReceiver, RSSender } from "../config/endpoints/settings";
import { CustomerStatusType } from "../utils/DataDashboardUtils";

const base = `${config.API_ROOT}/settings/organizations`;
const getSender = (org: string, sender: string) => `${base}/${org}/senders/${sender}`;
const testSender = getSender("testOrg", "testSender");
const firstSender = getSender("firstOrg", "firstSender");

export const dummySender: RSSender = {
    name: "testSender",
    organizationName: "testOrg",
    format: "CSV",
    topic: "covid-19",
    customerStatus: "testing",
    schemaName: "test/covid-19-test",
    allowDuplicates: false,
    processingType: "sync",
    version: 0,
    createdAt: "",
    createdBy: "",
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
            version: 0,
            createdAt: "",
            createdBy: "",
        });
    }
    return senders;
};

export const dummySenders = sendersGenerator(5);

/** TEST UTILITY - generates `RSReceiver[]`, each with a unique `name` (starting from "elr-0")
 *
 * @param count {number} How many unique receiverServices you want.
 * @param sort {boolean} Return results sorted alphabetically, defaults to false */
export const receiversGenerator = (count: number, sort?: boolean) => {
    const receiverServices: RSReceiver[] = [];
    for (let i = 0; i < count; i++) {
        receiverServices.push({
            name: `elr-${i}`,
            organizationName: "testOrg",
            version: 0,
            createdAt: "",
            createdBy: "",
        });
    }
    // Used to test sorting
    receiverServices.push({
        name: `abc-1`,
        organizationName: "testOrg",
        version: 0,
        createdAt: "",
        createdBy: "",
    });

    // Used to test filter
    receiverServices.push({
        name: `abc-2`,
        organizationName: "testOrg",
        customerStatus: CustomerStatusType.INACTIVE,
        version: 0,
        createdAt: "",
        createdBy: "",
    });

    if (sort) return receiverServices.sort((a, b) => a.name.localeCompare(b.name));

    return receiverServices;
};

export const dummyReceivers = receiversGenerator(5, true);
export const dummyActiveReceiver = {
    name: `abc-1`,
    organizationName: "testOrg",
};

export const publicKeysGenerator = (apiKeyCount: number) => {
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

    const publicKey: RSApiKeysResponse = {
        orgName: "testOrg",
        keys: apiKeySet,
    };
    return publicKey;
};

export const dummyPublicKey = publicKeysGenerator(2);

const handlers = [
    http.get(base, () => {
        return HttpResponse.json([fakeOrg, fakeOrg, fakeOrg, fakeOrg], {
            status: 200,
        });
    }),
    http.get(testSender, () => {
        return HttpResponse.json(dummySender, { status: 200 });
    }),
    http.get(`${base}/testOrg/senders`, () => {
        return HttpResponse.json(dummySenders, { status: 200 });
    }),
    http.get(firstSender, () => {
        return HttpResponse.json(null, { status: 200 });
    }),
    http.get(`${base}/testOrg`, () => {
        return HttpResponse.json(fakeOrg, { status: 200 });
    }),
    http.get(`${base}/testOrg/receivers`, () => {
        return HttpResponse.json(dummyReceivers, { status: 200 });
    }),
    http.get(`${base}/testOrgNoReceivers/receivers`, () => {
        return HttpResponse.json([], { status: 200 });
    }),
    http.get(`${base}/testOrg/public-keys`, () => {
        return HttpResponse.json(dummyPublicKey, { status: 200 });
    }),
    http.post(`${base}/testOrg/public-keys`, ({ request }) => {
        if (!request.headers.get("authorization")?.includes("TOKEN")) {
            return HttpResponse.json(null, { status: 401 });
        }
        return HttpResponse.json(dummyPublicKey, { status: 200 });
    }),
];

export const orgServer = setupServer(...handlers);
