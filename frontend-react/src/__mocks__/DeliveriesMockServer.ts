import { rest } from "msw";
import { setupServer } from "msw/node";

import { RSDelivery, RSFacility } from "../config/endpoints/deliveries";
import config from "../config";

export const makeFacilityFixture = (
    identifier: number,
    overrides?: Partial<RSFacility>,
): RSFacility => ({
    facility: overrides?.facility || "Facility Fixture",
    location: overrides?.location || "DeliveriesMockServer.ts",
    CLIA: identifier.toString(),
    positive: overrides?.positive || 0,
    total: overrides?.total || 0,
});

export const makeFacilityFixtureArray = (count: number) => {
    const fixtures: RSFacility[] = [];
    for (let i = 0; i < count; i++) {
        fixtures.push(makeFacilityFixture(i));
    }
    return fixtures;
};

export const makeDeliveryFixture = (
    id: number,
    overrides?: Partial<RSDelivery>,
): RSDelivery => ({
    deliveryId: overrides?.deliveryId || 0,
    batchReadyAt: overrides?.batchReadyAt || "",
    expires: overrides?.expires || "",
    receiver: overrides?.receiver || "",
    reportId: id.toString() || "",
    topic: overrides?.topic || "",
    reportItemCount: overrides?.reportItemCount || 0,
    fileName: overrides?.fileName || "",
    fileType: overrides?.fileType || "CSV",
});
export const makeDeliveryFixtureArray = (count: number) => {
    const fixtures: RSDelivery[] = [];
    for (let i = 0; i < count; i++) {
        fixtures.push(makeDeliveryFixture(i));
    }
    return fixtures;
};

const handlers = [
    rest.get(
        `${config.API_ROOT}/waters/org/testOrg.testService/deliveries`,
        (req, res, ctx) => {
            if (
                !req.headers.get("authorization")?.includes("TOKEN") ||
                !req.headers.get("organization")
            ) {
                return res(ctx.status(401));
            }
            return res(
                ctx.status(200),
                ctx.json([
                    makeDeliveryFixture(1),
                    makeDeliveryFixture(2),
                    makeDeliveryFixture(3),
                ]),
            );
        },
    ),
    /* Successfully returns a Report */
    rest.get(
        `${config.API_ROOT}/waters/report/123/delivery`,
        (req, res, ctx) => {
            return res(ctx.status(200), ctx.json(makeDeliveryFixture(123)));
        },
    ),
    rest.get(
        `${config.API_ROOT}/waters/report/123/facilities`,
        (req, res, ctx) => {
            const testRes = [makeFacilityFixture(1), makeFacilityFixture(2)];
            return res(ctx.status(200), ctx.json(testRes));
        },
    ),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
export const deliveryServer = setupServer(...handlers);
