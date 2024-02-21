import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

export const senderSettingsUrl = (orgName: string, senderName: string) =>
    `/api/settings/organizations/${orgName}/senders/${senderName}`;

const handlers = [
    http.put(
        `http://localhost:3000${senderSettingsUrl("abbott", "user1234")}`,
        () => {
            return HttpResponse.json(null, { status: 200 });
        },
    ),
];

export const settingsServer = setupServer(...handlers);
