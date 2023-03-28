import { without } from "lodash";
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

/**
 * Return all params not found in provided path that was found in
 * handler.
 */
export function getUnusedParams(path: string, handler: RestHandler) {
    const origParamKeys = Array.from(
        handler.info.path.toString().matchAll(/:\w+/g)
    ).map((m) => m[0]);
    const pathParamKeys = Array.from(path.matchAll(/:\w+/g)).map((m) => m[0]);
    const unusedParams = without(origParamKeys, ...pathParamKeys);

    return unusedParams;
}

/**
 * Wrapper for a RestHandler to allow for re-directing paths and allowing
 * custom resolver logic before return. RestHandler instance does not allow
 * access to its path or resolver for modification so we make a ProxyRestHandler
 * instead that can be passed to msw.
 */
export class ProxyRestHandler extends RestHandler {
    target: RestHandler;
    proxyResolver: any;

    constructor(
        method: RestHandlerMethod,
        path: Path,
        target: RestHandler,
        resolver?: ResponseResolver<any, any>
    ) {
        const unusedParams = getUnusedParams(path.toString(), target);
        if (unusedParams.length > 0) {
            throw new Error(
                `The following parameters were not found in path: ${unusedParams.join(
                    ", "
                )}`
            );
        }
        // Need a callback to get access to req, res, and ctx to pass
        // to our parent resolver once we run target handler
        const callback = (
            req: RestRequest,
            res: ResponseComposition<any>,
            ctx: RestContext
        ) => this.resolve(req, res, ctx);
        super(method, path, callback as ResponseResolver<any, any>);
        this.target = target;
        this.proxyResolver = resolver ?? defaultProxyResolver;
    }

    /**
     * Run our target handler and set the response inside context for our
     * parent resolver.
     */
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

    /**
     * Translate proxy path -> target path by properly placing param
     * values. Ex: /foo/bar/:id -> /bar/foo/:id - /foo/bar/123 -> /bar/foo/123
     */
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
