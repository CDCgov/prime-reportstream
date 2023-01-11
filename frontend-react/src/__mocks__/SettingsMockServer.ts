import { rest } from "msw";
import { setupServer } from "msw/node";

import { RSService } from "../config/endpoints/settings";

export const senderSettingsUrl = (orgName: string, senderName: string) =>
    `/api/settings/organizations/${orgName}/senders/${senderName}`;

const testService: RSService = {
    customerStatus: "testing",
    name: "test-service",
    organizationName: "test-org",
    topic: "test-topic",
};

const handlers = [
    rest.put(
        `http://localhost:3000${senderSettingsUrl("abbott", "user1234")}`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        }
    ),
    rest.get(
        "https://test.prime.cdc.gov/api/settings/organizations/test-org/senders",
        (req, res, context) => {
            return res(
                context.status(200),
                context.json([testService, testService])
            );
        }
    ),
    rest.get(
        "https://test.prime.cdc.gov/api/settings/organizations/test-org/receivers",
        (req, res, context) => {
            return res(
                context.status(200),
                context.json([testService, testService])
            );
        }
    ),
];

export const settingsServer = setupServer(...handlers);
