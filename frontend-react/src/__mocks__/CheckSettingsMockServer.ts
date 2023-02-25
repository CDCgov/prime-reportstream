import { rest } from "msw";
import { setupServer } from "msw/lib/node";

import { checkSettingsEndpoints } from "../config/endpoints/checkSettings";

const handlers = [
    rest.get(checkSettingsEndpoints.checkReceiver.url, (_req, res, ctx) => {
        return res(
            ctx.json({ result: "success", message: "mock" }),
            ctx.status(200)
        );
    }),
];

export const checkSettingsServer = setupServer(...handlers);
