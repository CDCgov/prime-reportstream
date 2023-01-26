import { rest, RestHandler } from "msw";

import { RSEndpoint } from "../RSEndpoint";

export interface RSEndpointGroupItem {
    endpoint: RSEndpoint<any>;
    meta: {
        [k: string]: {
            request?: unknown;
            response: unknown;
        };
    };
}

export class RSEndpointGroup {
    endpoints: Map<RSEndpoint<any>, RestHandler[]>;

    constructor(items: RSEndpointGroupItem[]) {
        this.endpoints = new Map();

        for (let item of items) {
            this.endpoints.set(
                item.endpoint,
                this.createEndpointHandlers(item)
            );
        }
    }

    createEndpointHandlers(item: RSEndpointGroupItem) {
        return Object.keys(item.meta).map((m) =>
            rest[m.toLocaleLowerCase() as keyof typeof rest](
                item.endpoint.url,
                (req, res, ctx) => {
                    const mockResponse = item.meta[m]?.response ?? {};
                    return res(ctx.json(mockResponse), ctx.status(200));
                }
            )
        );
    }
}
