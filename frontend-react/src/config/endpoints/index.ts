import { Method, AxiosRequestConfig } from "axios";
import omit from "lodash.omit";

import { StringIndexed } from "../../utils/UsefulTypes";
import config from "..";

const { API_ROOT } = config;

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

export type RSApiEndpoints = {
    [endpointName: string]: RSEndpoint;
};

export class RSEndpoint {
    path: string;
    method: Method;
    // optionally used for identifying this endpoint's usage in useQuery hooks
    queryKey?: string;

    constructor(params: EndpointConfig) {
        this.path = params.path;
        this.method = params.method;
        this.queryKey = params.queryKey || undefined;
    }

    get url(): string {
        return `${API_ROOT}${this.path}`;
    }

    get hasDynamicSegments(): boolean {
        return this.path.indexOf("/:") > -1;
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
            method: this.method,
            url: dynamicUrl,
        };
    }
}
