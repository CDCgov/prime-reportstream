import { AxiosRequestConfig } from "axios";

import {
    getStoredOktaToken,
    getStoredOrg,
} from "../../components/GlobalContextProvider";

export interface Endpoint {
    url: string;
    api: typeof Api;
}

export abstract class Api {
    /* STATIC MEMBERS */
    static accessToken: string = getStoredOktaToken();
    static organization: string = getStoredOrg();
    static baseUrl: string = "/api";

    /*
        This global config handles most use cases in which we
        call the API. This can be overridden in a child class
        as needed. 
    */
    static config: AxiosRequestConfig = {
        baseURL: `${process.env.REACT_APP_BACKEND_URL}`,
        headers: {
            Authorization: `Bearer ${this.accessToken}`,
            Organization: this.organization,
        },
        responseType: "json",
    };

    /*
        Super convenient way to generate an Endpoint in child classes!
        ChildClass.generateEndpoint(this.baseUrl, this)
    */
    static generateEndpoint(url: string, api: typeof Api): Endpoint {
        return {
            url: url,
            api: api,
        };
    }

    /* TODO: A generic test response generator like what we've made for HistoryApi */
}
