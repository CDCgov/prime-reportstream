import { rest } from "msw";
import { setupServer } from "msw/node";

export const senderSettingsUrl = (orgName: string, senderName: string) =>
    `/api/settings/organizations/${orgName}/senders/${senderName}`;

const handlers = [
    rest.put(
        `http://localhost:3000${senderSettingsUrl("abbott", "user1234")}`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        },
    ),
];

export const settingsServer = setupServer(...handlers);
