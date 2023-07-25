import { rest } from "msw";
import { setupServer } from "msw/node";

import config from "../config";
import { RSReceiver } from "../config/endpoints/settings";
import {
    RSReceiverDelivery,
    RSReceiverDeliveryResponse,
} from "../config/endpoints/dataDashboard";

const base = `${config.API_ROOT}/v1/receivers`;

/** TEST UTILITY - generates `RSReceiver[]`, each with a unique `name` (starting from "elr-0")
 *
 * @param count {number} How many unique receiverServices you want. */
export const receiverServicesGenerator = (count: number) => {
    const receiverServices: RSReceiver[] = [];
    for (let i = 0; i < count; i++) {
        receiverServices.push({
            name: `elr-${i}`,
            organizationName: "testOrg",
        });
    }
    return receiverServices;
};

/** TEST UTILITY - generates `RSReceiverDelivery[]`
 *
 * @param id {string} Used to generate reportId. */
export const makeRSReceiverDeliveryFixture = (
    id: number,
    overrides?: Partial<RSReceiverDelivery>
): RSReceiverDelivery => ({
    orderingProvider: overrides?.orderingProvider || "",
    orderingFacility: overrides?.orderingFacility || "",
    submitter: overrides?.submitter || "",
    reportId: id.toString() || "",
    createdAt: overrides?.createdAt || "",
    expirationDate: overrides?.expirationDate || "",
    testResultCount: overrides?.testResultCount || 0,
});
export const makeRSReceiverDeliveryFixtureArray = (count: number) => {
    const fixtures: RSReceiverDelivery[] = [];
    for (let i = 0; i < count; i++) {
        fixtures.push(makeRSReceiverDeliveryFixture(i));
    }
    return fixtures;
};

/** TEST UTILITY - generates `RSReceiverDeliveryResponse`, with the number of RSReceiverDelivery[] requested
 *
 * @param deliveryCount {number} How many unique RSReceiverDelivery you want. */
export const makeRSReceiverDeliveryResponseFixture = (
    deliveryCount: number,
    overrides?: Partial<RSReceiverDeliveryResponse>
): RSReceiverDeliveryResponse => ({
    meta: {
        type: overrides?.meta?.type || "delivery",
        totalCount: overrides?.meta?.totalCount || 101,
        totalFilteredCount: overrides?.meta?.totalFilteredCount || 101,
        totalPages: overrides?.meta?.totalPages || 10,
        nextPage: overrides?.meta?.nextPage || 2,
    },
    data: makeRSReceiverDeliveryFixtureArray(deliveryCount),
});

const handlers = [
    rest.post(`${base}/testOrg.testService/deliveries`, (req, res, ctx) => {
        if (!req.headers.get("authorization")?.includes("TOKEN")) {
            return res(ctx.status(401));
        }
        return res(
            ctx.status(200),
            ctx.json([makeRSReceiverDeliveryResponseFixture(5)])
        );
    }),
];

export const dataDashboardServer = setupServer(...handlers);
