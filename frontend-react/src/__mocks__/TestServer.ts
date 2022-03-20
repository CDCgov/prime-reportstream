import { rest } from "msw";
import { setupServer } from "msw/node";

import { sampleApi, SampleObject } from "../network/_sample/SampleApi";
import { EndpointConfig } from "../network/api/Api";

const obj = new SampleObject("string", true, 123);

const getEndpoint = sampleApi.getSampleList();
const postEndpoint = sampleApi.postSampleItem(obj);
const patchEndpoint = sampleApi.patchSampleItem(123, {
    bool: false,
});
const deleteEndpoint = sampleApi.deleteSampleItem(123);
const deleteEndpointFail = sampleApi.deleteSampleItem(124);

const handlers = [
    rest.get(getEndpoint.url, (req, res, ctx) => {
        return res(ctx.json([obj, obj]), ctx.status(200));
    }),

    rest.post(postEndpoint.url, (req, res, ctx) => {
        return res(ctx.json(postEndpoint.data), ctx.status(202));
    }),

    rest.patch<EndpointConfig<Partial<SampleObject>>>(
        patchEndpoint.url,
        (req, res, ctx) => {
            return res(ctx.json({ received: req.body.data }), ctx.status(202));
        }
    ),

    rest.delete(deleteEndpoint.url, (req, res, ctx) => {
        return res(ctx.status(200));
    }),

    rest.delete(deleteEndpointFail.url, (req, res, ctx) => {
        return res(ctx.status(404));
    }),
];

export const sampleServer = setupServer(...handlers);
