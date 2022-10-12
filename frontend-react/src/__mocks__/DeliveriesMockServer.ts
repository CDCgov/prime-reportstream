import { rest } from "msw";
import { setupServer } from "msw/node";

import { RSDelivery } from "../network/api/History/Reports";
import { RSFacility } from "../config/endpoints/deliveries";
import config from "../config";

const { RS_API_URL } = config;

const makeFacilityFixture = (
    identifier: number,
    overrides?: Partial<RSFacility>
): RSFacility => ({
    facility: overrides?.facility || "Facility Fixture",
    location: overrides?.location || "DeliveriesMockServer.ts",
    CLIA: identifier.toString(),
    positive: overrides?.positive || 0,
    total: overrides?.total || 0,
});

const handlers = [
    rest.get(
        "https://test.prime.cdc.gov/api/waters/org/testOrg.testService/deliveries",
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
                    new RSDelivery({ reportId: "1" }),
                    new RSDelivery({ reportId: "2" }),
                    new RSDelivery({ reportId: "3" }),
                ])
            );
        }
    ),
    /* Successfully returns a Report */
    rest.get(
        "https://test.prime.cdc.gov/api/waters/report/123/delivery",
        (req, res, ctx) => {
            return res(
                ctx.status(200),
                ctx.json(new RSDelivery({ reportId: "123" }))
            );
        }
    ),
    rest.get(
        `${RS_API_URL}/api/waters/report/123/facilities`,
        (req, res, ctx) => {
            const testRes = [makeFacilityFixture(1), makeFacilityFixture(2)];
            return res(ctx.status(200), ctx.json(testRes));
        }
    ),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
export const deliveryServer = setupServer(...handlers);
