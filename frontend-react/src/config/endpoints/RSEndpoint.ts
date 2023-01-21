import { QueryFunctionContext } from "@tanstack/react-query";
import axios, { AxiosRequestConfig } from "axios";
import { omit } from "lodash";

import { RequiredProps, StringIndexed } from "../../utils/UsefulTypes";
import { getMembershipsFromToken } from "../../hooks/UseOktaMemberships";
import { OKTA_AUTH } from "../../okta";
import {
    AppInsightsHeaders,
    getAppInsightsHeaders,
} from "../../TelemetryService";
import config from "../";

export const RSAuthenticationTypes = {
    OKTA: "okta",
} as const;
export type RSAuthenticationType =
    typeof RSAuthenticationTypes[keyof typeof RSAuthenticationTypes];

export interface RSRequestHeaders extends AppInsightsHeaders {
    "authentication-type"?: RSAuthenticationType;
    authorization?: string;
    organization?: string;
    [k: string]: undefined | string | string[] | number | boolean | null;
}

export type RSRequestConfig = Omit<
    RequiredProps<AxiosRequestConfig, "url">,
    "headers"
> & { headers: RSRequestHeaders };

export async function getRSRequestHeaders(
    headers?: Partial<RSRequestHeaders>
): Promise<RSRequestHeaders> {
    const accessToken = (await OKTA_AUTH.tokenManager.getTokens()).accessToken;
    // Isn't membership a mandatory thing? Should membershipsFromToken throw an error
    // if not none found so usages of it can safely assume a non-undefined return?
    // Assuming we'll never get an empty string for now.
    const { activeMembership } = getMembershipsFromToken(accessToken);
    const organization = activeMembership?.parsedName ?? "";

    let apiHeaders: RSRequestHeaders = { ...getAppInsightsHeaders() };

    if (accessToken) {
        apiHeaders = {
            ...apiHeaders,
            "authentication-type": "okta",
            authorization: `Bearer ${accessToken.accessToken}`,
            organization: organization,
        };
    }

    apiHeaders = { ...apiHeaders, ...headers };

    return apiHeaders;
}

const { API_ROOT } = config;

// odd that there isn't already a useable implementation of this somewhere
// or is there? where should this live?
export const HTTPMethods = {
    GET: "GET",
    POST: "POST",
    PUT: "PUT",
    DELETE: "DELETE",
    PATCH: "PATCH",
} as const;
export type HTTPMethod = typeof HTTPMethods[keyof typeof HTTPMethods];

/**
 * Copied from react-router for internal use with Endpoints.
 * Please use react-router's types directly for react-router
 * work.
 */
export type ParamParseSegment<Segment extends string> = Segment extends `${infer LeftSegment}/${infer RightSegment}` ? ParamParseSegment<LeftSegment> extends infer LeftResult ? ParamParseSegment<RightSegment> extends infer RightResult ? LeftResult extends string ? RightResult extends string ? LeftResult | RightResult : LeftResult : RightResult extends string ? RightResult : ParamParseFailed : ParamParseFailed : ParamParseSegment<RightSegment> extends infer RightResult ? RightResult extends string ? RightResult : ParamParseFailed : ParamParseFailed : Segment extends `:${infer Remaining}` ? Remaining : ParamParseFailed;
export type ParamParseKey<Segment extends string> = ParamParseSegment<Segment> extends string ? ParamParseSegment<Segment> : string;
/**
 * The parameters that were parsed from the URL path.
 */
export type Params<Key extends string = string> = {
    readonly [key in Key]: string | undefined;
};
export type ParamParseFailed = {
    failed: true;
};

export interface EndpointConfig {
    path: string;
    methods: {
        [key in HTTPMethod]?: unknown;
    };
    queryKey: string;
}

export interface EndpointConfigMap {
    [k: string]: EndpointConfig;
}

export interface AxiosOptionsWithSegments
    extends Omit<AxiosRequestConfig, "method" | "headers"> {
    method: HTTPMethod;
    segments?: StringIndexed<string>;
    headers?: Partial<RSRequestHeaders>;
}

export type RSEndpoints = {
    [endpointName: string]: RSEndpoint<any>;
};

export type EndpointMethodResponse<
    M extends HTTPMethod,
    E extends EndpointConfig
> = E["methods"][M] extends () => any
    ? ReturnType<NonNullable<E["methods"][M]>>
    : never;

export type RSEndpointFetchers<E extends EndpointConfig> = {
    [P in keyof E["methods"]]: (
        args: Partial<Omit<AxiosOptionsWithSegments, "method">>
    ) => P extends HTTPMethod
        ? Promise<Exclude<E["methods"][P], undefined>>
        : never;
};

export type RSEndpointOptionsBase = {
    segments?: {
        [k: string]: undefined | string | boolean | number
    },
    params?: {
        [k: string]: undefined | string | boolean | number
    }
}

export type RSEndpointOptions<T extends RSEndpoint<any>> = T extends RSEndpoint<infer C> 
    ? ParamParseKey<C["path"]> extends `${any}`
        ? Omit<RSEndpointOptionsBase,"segments"> & {segments: Params<ParamParseKey<C["path"]>>}
        : Omit<RSEndpointOptionsBase,"segments">
    : never;

export class RSEndpoint<E extends Readonly<EndpointConfig> = any> {
    path: string;
    queryKey: string;
    fetchers: RSEndpointFetchers<E>;

    constructor(params: E extends Readonly<EndpointConfig> ? E : never) {
        this.path = params.path;
        this.queryKey = params.queryKey;
        this.fetchers = Object.fromEntries(
            Object.entries(params.methods).map(([k, _]) => [
                k,
                this.createFetcher(k as HTTPMethod),
            ])
        ) as RSEndpointFetchers<E>;
    }

    get url(): string {
        return `${API_ROOT}${this.path}`;
    }

    get hasDynamicSegments(): boolean {
        return this.path.indexOf("/:") > -1;
    }

    createFetcher(method: HTTPMethod) {
        return async (
            args: Partial<Omit<AxiosOptionsWithSegments, "method">>
        ) => {
            const options = await this.toRSConfig({ ...args, method });

            if (!options.headers.authorization) {
                console.warn(
                    `Unauthenticated request to '${this.url}'\n Options:`,
                    options,
                    `\n Endpoint: `,
                    this
                );
            }

            const res = await axios(options);
            return res.data;
        };
    }

    async queryFn(context: QueryFunctionContext): Promise<E["methods"]["GET"]> {
        const args = context.queryKey;
        let options = {};
        if (Array.isArray(args)) {
            const lastArg = args[args.length - 1];
            if (lastArg.segments || lastArg.params) {
                options = lastArg;
            }
        }
        return await this.fetchers[HTTPMethods.GET]?.(options);
    }

    // replaces dynamic paths (`/:` prefixed segments) in an endpoint path
    // with supplied dynamic segment values.
    // ex. an endpoint with the path `/:hello` called with { hello: 'world' }
    // would return `/world`
    toDynamicUrl(segments?: StringIndexed<string>) {
        if (!segments && this.hasDynamicSegments) {
            throw new Error(
                `Attempted to use dynamic url without providing segment values: ${this.path}`
            );
        }
        if (!segments) {
            return this.url;
        }
        const pathWithSegments = Object.entries(segments).reduce(
            (pathWithSegments, [segmentKey, segmentValue]) =>
                pathWithSegments.replace(`:${segmentKey}`, segmentValue),
            this.url
        );
        if (pathWithSegments.indexOf("/:") > -1) {
            throw new Error(
                `missing dynamic path param: ${this.url}, ${segments}`
            );
        }
        return pathWithSegments;
    }

    // return the complete params that will be passed to axios to make a specific call to this endpoint
    toAxiosConfig(
        requestOptions: Partial<AxiosOptionsWithSegments>
    ): Partial<AxiosRequestConfig> {
        const dynamicUrl = this.toDynamicUrl(requestOptions.segments);
        return {
            ...omit(requestOptions, "segments"), // this is yucky but necessary for now
            url: dynamicUrl,
        };
    }

    // return the complete params that will be passed to axios to make a specific call to this endpoint
    async toRSConfig({
        segments,
        ...requestOptions
    }: AxiosOptionsWithSegments): Promise<RSRequestConfig> {
        const headers = await getRSRequestHeaders(requestOptions?.headers);
        const dynamicUrl = this.toDynamicUrl(segments);
        return {
            ...requestOptions,
            url: dynamicUrl,
            headers,
        };
    }
}

export function createEndpoints<T extends EndpointConfigMap>(meta: T) {
    return Object.fromEntries(
        Object.entries(meta).map(([k, m]) => [k, new RSEndpoint(m)])
    ) as unknown as { [P in keyof T]: RSEndpoint<T[P]> };
}
