import { AxiosRequestConfig, AxiosRequestHeaders } from "axios";

/* Type alias for CRUD ops that I wish to allow */
export type HTTPMethod = "POST" | "GET" | "PATCH" | "DELETE";

/* Parameters to instantiate an ApiConfig
 *
 * @property basePath: string
 * @property headers: AxiosRequestHeaders */
export interface ApiConfigProperties {
    root: string;
    headers: AxiosRequestHeaders;
}

/* Overriding the `method` property with my own type. */
export interface EndpointConfig<T> extends AxiosRequestConfig<T> {
    method: HTTPMethod;
    url: string;
}

/* An ApiConfig houses methods needed to create an API class
 * A single ApiConfig should exist for each unique base URLs
 * you access. Any endpoints can be housed in their own Api
 * class.
 *
 * @param config: ConfigProperties */
export class ApiConfig {
    constructor(config: ApiConfigProperties) {
        this.root = config.root;
        this.headers = config.headers;
    }
    root: string;
    headers: AxiosRequestHeaders;

    /* Prepend API base path (e.g. http://localhost:8080) */
    url = (s: string) => `${this.root}/${s}`;
}

/* An Api houses methods that return super.configure(params),
 * known as EndpointConfigs
 *
 * @param apiConfig:  */
export class Api {
    constructor(apiConfig: ApiConfig, basePath: string) {
        this.config = apiConfig;
        this.basePath = basePath;
    }
    config: ApiConfig;
    basePath: string;

    /* Handles configuration logic */
    configure<D>(params: EndpointConfig<D>): EndpointConfig<D> {
        return {
            ...params, // Spread first and then override below

            /* Value overrides */
            url: this.config.url(params.url),
            method: params.method || "GET", // Default to "GET" method
            headers: params.headers || this.config.headers, // Override headers or default to base headers
            responseType: params.responseType || "json", // Default "json" response
        };
    }
}
