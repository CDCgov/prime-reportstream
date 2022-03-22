import { rest } from "msw";
import { setupServer } from "msw/node";

import { dummySender } from "../hooks/UseSenderMode.test";

/* There's some .env shuffling going on that makes endpoint generation NOT match the
 * network call triggered by the test suite. Uses test.prime.cdc.gov
 * in the test renderer but the path comes out to localhost if using
 * env vars */
const handlers = [
    rest.get(
        `https://test.prime.cdc.gov/api/settings/organizations/testOrg/senders/testSender`,
        (req, res, ctx) => {
            return res(ctx.json(dummySender), ctx.status(200));
        }
    ),
    rest.get(
        `https://test.prime.cdc.gov/api/settings/organizations/firstOrg/senders/firstSender`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        }
    ),
];

export const orgServer = setupServer(...handlers);
