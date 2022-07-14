import { AxiosRequestConfig, AxiosRequestHeaders, Method } from "axios";

import { Newable, SimpleError, StringIndexed } from "../../utils/UsefulTypes";

/* Using the utility type `Omit<T, ...>` we can limit what methods we allow if needed.
 * Example: AvailableMethods = Omit<Method, "DELETE">[] */
export type AvailableMethods = Method[];
export class Endpoint {
    url: string;
    methods: AvailableMethods;
    constructor(url: string, methods: AvailableMethods) {
        this.url = url;
        this.methods = methods;
    }
}
/* Name your endpoints! */
export type EndpointMap = Map<string, Endpoint>;
export class API {
    resource: Newable<any>; // Resource class
    baseUrl: string;
    endpoints: EndpointMap = new Map();
    constructor(resource: Newable<any>, baseUrl: string) {
        this.resource = resource;
        this.baseUrl = baseUrl;
    }
    addEndpoint(name: string, url: string, methods: AvailableMethods): this {
        this.endpoints.set(name, new Endpoint(url, methods));
        return this;
    }
}

/* Make some headers required */
export interface RSRequestHeaders extends AxiosRequestHeaders {
    "authentication-type": string;
    authorization: string;
    organization: string;
}
/* Make some fields required or overwrite types */
export interface RSRequestConfig extends AxiosRequestConfig {
    url: string;
    method: Method;
    headers: RSRequestHeaders;
}
/* Prevents overriding RSRequestConfig values */
export type AdvancedConfig<D> = Omit<
    AxiosRequestConfig<D>,
    "url" | "method" | "headers"
>;
/* Basic state for API calls */
export interface BasicAPIResponse<T> {
    data: T;
    error: string;
    loading: boolean;
    trigger: () => void;
}
/* Safe endpoint extraction */
const extractEndpoint = (api: API, key: string): Endpoint => {
    const endpoint: Endpoint | undefined = api.endpoints.get(key);
    if (!endpoint)
        throw Error(`You must provide a valid endpoint key: ${key} not found`);
    return endpoint;
};

/* Called from consumer hook to build URL. Parameters for endpoints should be passed
 * through the consumer hook and into this function when building the URL. */
export const buildEndpointUrl = <P extends StringIndexed>(
    api: API,
    endpointKey: string,
    parameters?: P
): string => {
    try {
        /* Will throw on failure */
        const endpoint = extractEndpoint(api, endpointKey);
        /* Slashes NOT built in! declare urls with leading slash */
        const construct = (endpointUrl: string) =>
            `${process.env.REACT_APP_BACKEND_URL}${api.baseUrl}${endpointUrl}`;
        /* Checks for params by looking for the colon as an indicator */
        if (endpoint.url.includes(":")) {
            if (parameters === undefined) {
                throw Error(
                    `Parameters are required for ${endpointKey}: ${endpoint.url}`
                );
            } else {
                const endpointUrl = endpoint.url
                    /* split at every slash */
                    .split("/")
                    /* replace `:param` string with param value */
                    .map(
                        (key) =>
                            key.includes(":")
                                ? parameters?.[key.slice(1)] || key // for parameters w/ safe default
                                : key // for non-parameters
                    )
                    /* rejoin as url string */
                    .join("/");
                return construct(endpointUrl);
            }
        } else {
            return construct(endpoint.url);
        }
    } catch (e: any) {
        /* Catching extractEndpoints error, or anything form here, and piping it up */
        console.error(e.message);
        throw Error(e.message);
    }
};

/* Ensure the endpoint chosen has access to the method desired. If not,
 * this will throw and the error will be communicated in the output of
 * generateRequestConfig */
export const endpointHasMethod = (
    api: API,
    endpointKey: string,
    method: Method
) => {
    const endpoint = extractEndpoint(api, endpointKey);
    const canAccessMethod = endpoint.methods
        .map((m) => m.toUpperCase())
        .includes(method.toUpperCase());
    if (!canAccessMethod)
        throw Error(`Method ${method} cannot be used by ${endpointKey}`);
};

/* Handles generating the config from inputs with checks in the middle. If
 * any checks fail, this will return a SimpleError. You can check this by
 * calling `x instanceof SimpleError`.
 *
 * Both buildEndpointUrl and endpointHasMethod will throw if inputs lead to
 * errors, and that is communicated through SimpleError.message */
export const createRequestConfig = <P extends StringIndexed, D = any>(
    api: API,
    endpointKey: string,
    method: Method,
    token?: string,
    organization?: string,
    parameters?: P,
    // Allows us to use more of AxiosRequestConfig if we want
    advancedConfig?: AdvancedConfig<D>
): RSRequestConfig | SimpleError => {
    try {
        endpointHasMethod(api, endpointKey, method);
        return {
            url: buildEndpointUrl(api, endpointKey, parameters),
            method: method,
            headers: {
                "authentication-type": "okta",
                authorization: `Bearer ${token || ""}`,
                organization: `${organization || ""}`,
            },
            ...advancedConfig,
        };
    } catch (e: any) {
        return new SimpleError(e.message);
    }
};
