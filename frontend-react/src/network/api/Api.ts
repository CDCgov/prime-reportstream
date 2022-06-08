import { AxiosRequestConfig, AxiosRequestHeaders } from "axios";

import { registerApi } from "../Apis";

/* Type alias for CRUD ops that I wish to allow */
export type HTTPMethod = "POST" | "GET" | "PATCH" | "DELETE";

export const API_ROOT = `${process.env.REACT_APP_BACKEND_URL}/api`;

/* Overriding the `method` property with my own type. */
export interface EndpointConfig<T> extends AxiosRequestConfig<T> {
    method: HTTPMethod;
    url: string;
}

/* An Api houses methods that return super.configure(params),
 * known as EndpointConfigs. Each Api instance also contains methods that will
 *  - register itself with the application
 *  - update its own auth headers (session)
 * @param basePath: the string representing the root path (not host) for a given API  */
export class Api {
    constructor(basePath: string) {
        this.basePath = basePath;
        this.root = API_ROOT;
        this.headers = {};
        this.register();
    }
    basePath: string;
    headers: AxiosRequestHeaders;
    root: string;

    /* Prepend API base path (e.g. http://localhost:8080) */
    generateUrl = (s: string) => `${this.root}/${s}`;

    /* Handles configuration logic */
    // todo: make it so that this doesn't need to be run on every request
    configure<D>(params: EndpointConfig<D>): EndpointConfig<D> {
        return {
            ...params, // Spread first and then override below

            /* Value overrides */
            url: this.generateUrl(params.url),
            method: params.method || "GET", // Default to "GET" method
            headers: params.headers || this.headers, // Override headers or default to base headers
            responseType: params.responseType || "json", // Default "json" response
        };
    }

    updateSession(headers: AxiosRequestHeaders) {
        this.headers = headers;
    }

    register() {
        registerApi(this);
    }

    deregister() {
        // we may not need this
    }
}
