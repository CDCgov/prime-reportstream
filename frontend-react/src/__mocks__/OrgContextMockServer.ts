import {Endpoint} from "../network/api/Api";
import {OrgApi} from "../network/api/OrgApi";
import {rest} from "msw";
import {dummyOrg} from "../contexts/OrgContext.test";
import {setupServer} from "msw/node";

const detailEndpoint: Endpoint = OrgApi.detail("ignore")

const handlers = [
    rest.get(
        `${process.env.REACT_APP_BACKEND_URL}${detailEndpoint.url}`,
        (req, res, ctx) => {
            return res(ctx.json(dummyOrg), ctx.status(200))
        }
    )
]

export const orgServer = setupServer(...handlers)
