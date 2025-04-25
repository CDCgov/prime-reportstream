import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

import config from "../config";
import { RSDelivery, RSFacility } from "../config/endpoints/deliveries";

export const makeFacilityFixture = (identifier: number, overrides?: Partial<RSFacility>): RSFacility => ({
    facility: overrides?.facility ?? "Facility Fixture",
    location: overrides?.location ?? "DeliveriesMockServer.ts",
    CLIA: identifier.toString(),
    positive: overrides?.positive ?? 0,
    total: overrides?.total ?? 0,
});

export const makeFacilityFixtureArray = (count: number) => {
    const fixtures: RSFacility[] = [];
    for (let i = 0; i < count; i++) {
        fixtures.push(makeFacilityFixture(i));
    }
    return fixtures;
};

export const makeDeliveryFixture = (id: number, overrides?: Partial<RSDelivery>): RSDelivery => ({
    deliveryId: overrides?.deliveryId ?? 0,
    batchReadyAt: overrides?.batchReadyAt ?? "",
    expires: overrides?.expires ?? "",
    receiver: overrides?.receiver ?? "",
    reportId: id.toString() || "",
    topic: overrides?.topic ?? "",
    reportItemCount: overrides?.reportItemCount ?? 0,
    fileName: overrides?.fileName ?? "",
    fileType: overrides?.fileType ?? "CSV",
});
export const makeDeliveryFixtureArray = (count: number) => {
    const fixtures: RSDelivery[] = [];
    for (let i = 0; i < count; i++) {
        fixtures.push(makeDeliveryFixture(i));
    }
    return fixtures;
};

const handlers = [
    http.get(`${config.API_ROOT}/waters/org/testOrg.testService/deliveries`, ({ request }) => {
        if (!request.headers.get("authorization")?.includes("TOKEN") || !request.headers.get("organization")) {
            return HttpResponse.json(null, { status: 401 });
        }
        return HttpResponse.json([makeDeliveryFixture(1), makeDeliveryFixture(2), makeDeliveryFixture(3)], {
            status: 200,
        });
    }),
    /* Successfully returns a Report */
    http.get(`${config.API_ROOT}/waters/report/123/delivery`, () => {
        return HttpResponse.json(makeDeliveryFixture(123), { status: 200 });
    }),
    http.get(`${config.API_ROOT}/waters/report/123/facilities`, () => {
        const testRes = [makeFacilityFixture(1), makeFacilityFixture(2)];
        return HttpResponse.json(testRes, { status: 200 });
    }),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
export const deliveryServer = setupServer(...handlers);
