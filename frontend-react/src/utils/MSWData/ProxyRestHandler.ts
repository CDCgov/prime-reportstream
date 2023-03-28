import {
    ResponseResolver,
    RestHandler,
    Path,
    MockedRequest,
    ResponseComposition,
    RestContext,
    RestRequest,
} from "msw";

export type RestHandlerMethod = string | RegExp;

export interface ProxyRequestContext extends RestContext {
    proxy: {
        res?: any;
        error?: Error;
    };
}

export async function defaultProxyResolver(_: any, _1: any, ctx: any) {
    if (ctx.proxy.error) {
        throw ctx.proxy.error;
    }
    return ctx.proxy.res;
}

export class ProxyRestHandler extends RestHandler {
    target: RestHandler;
    proxyResolver: any;

    constructor(
        method: RestHandlerMethod,
        path: Path,
        target: RestHandler,
        resolver?: ResponseResolver<any, any>
    ) {
        const callback = (
            req: RestRequest,
            res: ResponseComposition<any>,
            ctx: RestContext
        ) => this.resolve(req, res, ctx);
        super(method, path, callback as ResponseResolver<any, any>);
        this.target = target;
        this.proxyResolver = resolver ?? defaultProxyResolver;
    }

    async resolve(
        req: RestRequest,
        res: ResponseComposition<any>,
        ctx: RestContext
    ) {
        const resolved: ProxyRequestContext["proxy"] = {
            res: undefined,
            error: undefined,
        };
        const proxyReq = await this.createProxyRequest(req);

        try {
            resolved.res = (await this.target.run(proxyReq))?.response;
        } catch (e: any) {
            resolved.error = e;
        }

        const proxyCtx = {
            ...ctx,
            proxy: resolved,
        };

        return this.proxyResolver(req, res, proxyCtx);
    }

    async createProxyRequest(req: RestRequest) {
        const url = Object.entries(req.params).reduce(
            (str, [k, v]) => str.replace(`:${k}`, v.toString()),
            this.target.info.path.toString()
        );
        return new MockedRequest(new URL(url), {
            ...req,
            body: await req.arrayBuffer(),
        });
    }
}
