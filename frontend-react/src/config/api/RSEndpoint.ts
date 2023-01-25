import { QueryFunctionContext } from "@tanstack/react-query";
import axios, { AxiosRequestConfig, RawAxiosRequestHeaders } from "axios";
import omit from "lodash.omit";

import { getMembershipsFromToken } from "../../hooks/UseOktaMemberships";
import { OKTA_AUTH } from "../../okta";
import {
    AppInsightsHeaders,
    getAppInsightsHeaders,
} from "../../TelemetryService";
import config from "..";
import { RSNetworkError } from "../../utils/RSNetworkError";

export const RSAuthenticationTypes = {
    OKTA: "okta",
} as const;
export type RSAuthenticationType =
    (typeof RSAuthenticationTypes)[keyof typeof RSAuthenticationTypes];

export interface RSRequestHeaders extends AppInsightsHeaders {
    "authentication-type"?: RSAuthenticationType;
    authorization?: string;
    organization?: string;
    [k: string]: undefined | string | number | boolean;
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
export type HTTPMethod = (typeof HTTPMethods)[keyof typeof HTTPMethods];

/**
 * Copied from react-router for internal use with Endpoints.
 * Please use react-router's types directly for react-router
 * work.
 */
export type ParamParseSegment<Segment extends string> =
    Segment extends `${infer LeftSegment}/${infer RightSegment}`
        ? ParamParseSegment<LeftSegment> extends infer LeftResult
            ? ParamParseSegment<RightSegment> extends infer RightResult
                ? LeftResult extends string
                    ? RightResult extends string
                        ? LeftResult | RightResult
                        : LeftResult
                    : RightResult extends string
                    ? RightResult
                    : ParamParseFailed
                : ParamParseFailed
            : ParamParseSegment<RightSegment> extends infer RightResult
            ? RightResult extends string
                ? RightResult
                : ParamParseFailed
            : ParamParseFailed
        : Segment extends `:${infer Remaining}`
        ? Remaining
        : ParamParseFailed;
export type ParamParseKey<Segment extends string> =
    ParamParseSegment<Segment> extends string
        ? ParamParseSegment<Segment>
        : string;
/**
 * The parameters that were parsed from the URL path.
 */
export type Params<Key extends string = string> = {
    readonly [key in Key]: string;
};
export type ParamParseFailed = {
    failed: true;
};

export interface EndpointConfig {
    path: string;
    methods: {
        [key in HTTPMethod]?: unknown;
    };
    params?: unknown;
    queryKey: string;
}

export interface EndpointConfigMap {
    [k: string]: EndpointConfig;
}

export interface AxiosOptionsWithSegments
    extends Omit<AxiosRequestConfig, "method" | "headers"> {
    method: HTTPMethod;
    segments?: StringIndexed<string>;
    headers?: Partial<RawAxiosRequestHeaders>;
}

export interface RSOptionsWithSegments
    extends Omit<AxiosRequestConfig, "method" | "headers"> {
    method: HTTPMethod;
    segments?: StringIndexed<string>;
    headers?: Partial<RSRequestHeaders>;
}

export type RSApiEndpoints = {
    [endpointName: string]: RSEndpoint<any>;
};

export type RSEndpointMethodRequestType<
    E extends EndpointConfig,
    M extends HTTPMethod
> = E["methods"][M] extends {} ? NonNullable<E["methods"][M]> : never;

export type RSEndpointFetchers<E extends EndpointConfig> = {
    [P in string & keyof E["methods"] as Lowercase<P>]: (
        args: Partial<Omit<RSOptionsWithSegments, "method">>
    ) => P extends HTTPMethod
        ? Promise<RSEndpointMethodRequestType<E, P>>
        : never;
};

export type RSEndpointParams<T extends RSEndpoint<EndpointConfig>> =
    T["meta"]["params"];

export type RSEndpointSegments<T extends RSEndpoint<EndpointConfig>> =
    ParamParseKey<T["meta"]["path"]> extends `${any}`
        ? Params<ParamParseKey<T["meta"]["path"]>>
        : never;

export type RSEndpointOptions<T extends RSEndpoint<EndpointConfig>> =
    RSEndpointSegments<T> extends never
        ? RSEndpointParams<T> extends never
            ? never
            : { params?: Partial<RSEndpointParams<T>> | any }
        : RSEndpointParams<T> extends never
        ? { segments?: RSEndpointSegments<T>; params?: any }
        : {
              segments?: Partial<RSEndpointSegments<T>>;
              params?: RSEndpointParams<T> | any;
          };

export function createPathName(path: string) {
    return path
        .replaceAll(":", "")
        .split("/")
        .map((f) => f[0].toLocaleUpperCase() + f.slice(1));
}

export class RSEndpoint<E extends EndpointConfig> {
    meta: E;
    fetchers: RSEndpointFetchers<E>;

    constructor(meta: E) {
        this.meta = meta;
        this.fetchers = {} as any;

        for (let method of Object.keys(this.meta.methods) as HTTPMethod[]) {
            this.addFetcher(method);
        }
    }

    get url(): string {
        return `${API_ROOT}${this.meta.path}`;
    }

    get hasDynamicSegments(): boolean {
        return this.meta.path.indexOf("/:") > -1;
    }

    addFetcher<T extends keyof E["methods"] & HTTPMethod>(method: T): void {
        this.fetchers[method.toLocaleLowerCase() as Lowercase<T>] = (async (
            args: Partial<Omit<RSOptionsWithSegments, "method">>
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

            try {
                const res = await axios<RSEndpointMethodRequestType<E, T>>(
                    options
                );
                return res.data;
            } catch (e: any) {
                throw new RSNetworkError(e);
            }
        }) as any;
    }

    queryFn = async (
        context: QueryFunctionContext
    ): Promise<E["methods"]["GET"]> => {
        if (!this.fetchers.get)
            throw new Error("Querying in endpoint not allowed");

        const args = context.queryKey;
        let options = {};
        if (Array.isArray(args)) {
            const lastArg = args[args.length - 1];
            if (lastArg?.segments || lastArg?.params) {
                options = lastArg;
            }
        }
        return await this.fetchers.get(options);
    };

    // replaces dynamic paths (`/:` prefixed segments) in an endpoint path
    // with supplied dynamic segment values.
    // ex. an endpoint with the path `/:hello` called with { hello: 'world' }
    // would return `/world`
    toDynamicUrl(segments?: StringIndexed<string>) {
        if (!segments && this.hasDynamicSegments) {
            throw new Error(
                `Attempted to use dynamic url without providing segment values: ${this.meta.path}`
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
    }: RSOptionsWithSegments): Promise<RSRequestConfig> {
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
