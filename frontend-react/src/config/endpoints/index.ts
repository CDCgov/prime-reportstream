import { Method, AxiosRequestConfig } from "axios";
import omit from "lodash.omit";

import { StringIndexed } from "../../utils/UsefulTypes";

// this should be contained in a config file
// TODO: move all config specific global variables to a config file
export const API_ROOT = `${process.env.REACT_APP_BACKEND_URL}/api`;

// odd that there isn't already a useable implementation of this somewhere
// or is there? where should this live?
export enum HTTPMethods {
    GET = "GET",
    POST = "POST",
    PUT = "PUT",
    DELETE = "DELETE",
    PATCH = "PATCH",
}

export interface EndpointConfig {
    path: string;
    method: Method;
    queryKey?: string;
}

export interface AxiosOptionsWithSegments extends AxiosRequestConfig {
    segments: StringIndexed<string>;
}

export class RSEndpoint {
    path: string;
    method: Method;
    queryKey?: string;

    constructor(params: EndpointConfig) {
        this.path = params.path;
        this.method = params.method;
        this.queryKey = params.queryKey || undefined;
    }

    get url(): string {
        return `${API_ROOT}${this.path}`;
    }

    toDynamicUrl(segments?: StringIndexed<string>) {
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
            method: this.method,
            url: dynamicUrl,
            ...omit(requestOptions, "segments"), // this is yucky
        };
    }
}
