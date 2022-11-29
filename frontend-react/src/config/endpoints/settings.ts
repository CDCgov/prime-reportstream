import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export enum ServicesUrls {
    SETTINGS = "/settings/organizations/:orgName",
    SENDERS = "/settings/organizations/:orgName/senders",
    SENDER_DETAIL = "/settings/organizations/:orgName/senders/:sender",
    RECEIVERS = "/settings/organizations/:orgName/receivers",
}

/** Response is much larger than this but not all of it is used for front-end yet */
export interface RSService {
    name: string;
    organizationName: string;
    topic?: string;
    customerStatus?: string;
}

export interface RSOrganizationSettings {
    createdAt: string;
    createdBy: string;
    description: string;
    filters: string[];
    jurisdiction: string;
    name: string;
    version: number;
    stateCode?: string;
    countyName?: string;
}

interface SenderKeys {
    scope: string;
    keys: {}[];
}

export interface RSSender extends RSService {
    allowDuplicates: boolean;
    createdAt?: string;
    createdBy?: string;
    customerStatus: string;
    format: string;
    keys?: SenderKeys;
    name: string;
    organizationName: string;
    primarySubmissionMethod?: string;
    processingType: string;
    schemaName: string;
    senderType?: string;
    topic: string;
    version?: number;
}

export interface RSReceiver extends RSService {}

/*
Services Endpoints
* senders -> fetches a list of organization's senders
* receivers -> fetches a list of organization's receivers
*/
export const servicesEndpoints: RSApiEndpoints = {
    settings: new RSEndpoint({
        path: ServicesUrls.SETTINGS,
        method: HTTPMethods.GET,
        queryKey: "servicesSettings",
    }),
    senders: new RSEndpoint({
        path: ServicesUrls.SENDERS,
        method: HTTPMethods.GET,
        queryKey: "servicesSenders",
    }),
    senderDetail: new RSEndpoint({
        path: ServicesUrls.SENDER_DETAIL,
        method: HTTPMethods.GET,
        queryKey: "servicesSenderDetail",
    }),
    receivers: new RSEndpoint({
        path: ServicesUrls.RECEIVERS,
        method: HTTPMethods.GET,
        queryKey: "servicesReceivers",
    }),
};
