import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

import config from "../config";
import { RSDeliveryHistory, RSDeliveryHistoryResponse } from "../config/endpoints/deliveries";

const base = `${config.API_ROOT}/v1/waters/org`;

/** TEST UTILITY - generates `RSDeliveryHistory[]`
 *
 * @param id {string} Used to generate reportId. */
export const makeRSReceiverDeliveryFixture = (
    id: number,
    overrides?: Partial<RSDeliveryHistory>,
): RSDeliveryHistory => ({
    deliveryId: overrides?.deliveryId ?? "0",
    createdAt: overrides?.createdAt ?? "",
    expiresAt: overrides?.expiresAt ?? "",
    receiver: overrides?.receiver ?? "",
    reportId: id.toString() || "",
    topic: overrides?.topic ?? "",
    reportItemCount: overrides?.reportItemCount ?? "3",
    fileName: overrides?.fileName ?? "",
    fileType: overrides?.fileType ?? "CSV",
    receivingOrgSvcStatus: overrides?.receivingOrgSvcStatus ?? "",
});
export const makeRSDeliveryFixtureArray = (count: number) => {
    const fixtures: RSDeliveryHistory[] = [];
    for (let i = 0; i < count; i++) {
        fixtures.push(makeRSReceiverDeliveryFixture(i));
    }
    return fixtures;
};

/** TEST UTILITY - generates `RSDeliveryHistoryResponse`, with the number of RSDeliveryHistory[] requested
 *
 * @param deliveryCount {number} How many unique RSDeliveryHistory you want. */
export const makeRSDeliveryHistoryResponseFixture = (
    deliveryCount: number,
    overrides?: Partial<RSDeliveryHistoryResponse>,
): RSDeliveryHistoryResponse => ({
    meta: {
        totalCount: overrides?.meta?.totalCount ?? 101,
        totalFilteredCount: overrides?.meta?.totalFilteredCount ?? 101,
        totalPages: overrides?.meta?.totalPages ?? 10,
        nextPage: overrides?.meta?.nextPage ?? 2,
        previousPage: overrides?.meta?.previousPage ?? 1,
    },
    data: makeRSDeliveryFixtureArray(deliveryCount),
});

const handlers = [
    http.post(`${base}/testOrg.testService/deliveries`, ({ request }) => {
        if (!request.headers.get("authorization")?.includes("TOKEN")) {
            return HttpResponse.json(null, { status: 401 });
        }
        return HttpResponse.json(makeRSDeliveryHistoryResponseFixture(5), {
            status: 200,
        });
    }),
];

export const deliveriesHistoryServer = setupServer(...handlers);
