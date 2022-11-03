import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    MessageListResource,
    messageTrackerEndpoints,
} from "../config/endpoints/messageTracker";

export const MOCK_MESSAGE_SENDER_DATA = [
    {
        messageId: "12-234567",
        sender: "somebody 1",
        submittedDate: "09/28/2022",
        reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
    },
    {
        messageId: "12-234567",
        sender: "somebody 2",
        submittedDate: "09/29/2022",
        reportId: "86c4c66f-3d99-4845-8bea-111210c05e63",
    },
    {
        messageId: "12-234567",
        sender: "somebody 3",
        submittedDate: "09/30/2022",
        reportId: "26a37945-ed12-4578-b4f6-203e8b9d62ce",
    },
];

const messageSearch = messageTrackerEndpoints.search.toDynamicUrl({
    messageId: "alaska1",
});

const messagesSearchResultList: MessageListResource[] =
    MOCK_MESSAGE_SENDER_DATA;

const handlers = [
    rest.get(messageSearch, (_req, res, ctx) => {
        return res(ctx.json(messagesSearchResultList), ctx.status(200));
    }),
];

export const messageTrackerServer = setupServer(...handlers);
