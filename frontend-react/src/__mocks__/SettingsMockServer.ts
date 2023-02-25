import { rest } from "msw";
import { setupServer } from "msw/node";

import config from "../config";

export const senderSettingsUrl = (orgName: string, senderName: string) =>
    `settings/organizations/${orgName}/senders/${senderName}`;

const handlers = [
    rest.put(
        `${config.RS_API_URL}/api/${senderSettingsUrl("abbott", "user1234")}`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        }
    ),
];

export const settingsServer = setupServer(...handlers);
