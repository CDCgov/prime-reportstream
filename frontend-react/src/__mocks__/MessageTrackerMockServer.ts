import { rest } from "msw";
import { setupServer } from "msw/node";

import config from "../config";
import { RSMessageDetail } from "../config/endpoints/messageTracker";

export const makeMessageDetailsFixture = (
    id: number,
    overrides?: Partial<RSMessageDetail>
): RSMessageDetail => ({
    id: id || 1,
    messageId: overrides?.messageId || "",
    sender: overrides?.sender || "",
    submittedDate: overrides?.submittedDate || "",
    reportId: overrides?.reportId || "e46339c7-408a-49aa-af4f-29712c8c20df",
    fileName: overrides?.fileName || "",
    fileUrl: overrides?.fileUrl || "",
    warnings: overrides?.warnings || [],
    errors: overrides?.errors || [],
    receiverData: overrides?.receiverData || [],
});

const { RS_API_URL } = config;
const handlers = [
    rest.get(`${RS_API_URL}/api/message/11`, (req, res, ctx) => {
        return res(ctx.status(200), ctx.json(makeMessageDetailsFixture(11)));
    }),
];
export const messageTrackerServer = setupServer(...handlers);
