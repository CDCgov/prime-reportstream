import { AxiosRequestConfig, AxiosRequestHeaders, Method } from "axios";

/* Available APIs from the RS API */
export enum ApiBaseUrls {
    HISTORY = "/api/history", // SAMPLE
    LOOKUP_TABLES = "/api/lookuptables",
}
export type AvailableMethods = Method[];
export interface Endpoint {
    url: string;
    methods: AvailableMethods;
}
/* Name your endpoints:
 * e.g. new Map([
 *   ["allOrgs", { url: "/organization", methods [GET] }],
 *   ["org", { url: "/organization/:id", methods: [GET, UPDATE] }]
 * ]) */
export type EndpointMap = Map<string, Endpoint>;
/* Declaration of an API */
export interface API {
    baseUrl: ApiBaseUrls;
    endpoints: EndpointMap;
}
/* Allows us to access them via string: params["id"] */
export interface RSUrlParams {
    [key: string]: any;
}
/* Make some headers required */
export interface RSRequestHeaders extends AxiosRequestHeaders {}
/* Make some fields required or overwrite types */
export interface RSRequestConfig extends AxiosRequestConfig {
    url: string;
    method: Method;
    headers: RSRequestHeaders;
}
/* Prevents overriding RSRequestConfig values */
export type AdvancedConfig = Omit<
    AxiosRequestConfig,
    "url" | "method" | "headers"
>;

/* Safe endpoint extraction */
const extractEndpoint = (api: API, key: string): Endpoint => {
    const endpoint: Endpoint | undefined = api.endpoints.get(key);
    if (!endpoint)
        throw Error(`You must provide a valid endpoint key: ${key} not found`);
    return endpoint;
};

/* Called from consumer hook to build URL. Parameters for endpoints should be passed
 * through the consumer hook and into this function when building the URL. */
export const buildEndpointUrl = <P extends RSUrlParams>(
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
        console.error(e.message);
        return "";
    }
};

/* Handles generating the config from parameters */
export const createAxiosConfig = <P extends RSUrlParams>(
    api: API,
    endpointKey: string,
    method: Method,
    token?: string,
    organization?: string,
    parameters?: P,
    // Allows us to use more of AxiosRequestConfig if we want
    advancedConfig?: AdvancedConfig
): RSRequestConfig => {
    const url = buildEndpointUrl(api, endpointKey, parameters);
    if (url === "") console.warn(`Looks like your url didn't parse!`);
    return {
        url: url,
        method: method,
        headers: {
            "authentication-type": "okta",
            authorization: `Bearer ${token || ""}`,
            organization: `${organization || ""}`,
        },
        ...advancedConfig,
    };
};
