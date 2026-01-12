import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export enum ServicesUrls {
    SETTINGS = "/settings/organizations/:orgName",
    SENDERS = "/settings/organizations/:orgName/senders",
    SENDER_DETAIL = "/settings/organizations/:orgName/senders/:sender",
    RECEIVERS = "/settings/organizations/:orgName/receivers",
    PUBLIC_KEYS = "/settings/organizations/:orgName/public-keys",
}

export interface RSSettings {
    version: number;
    createdAt: string;
    createdBy: string;
}

/** Response is much larger than this but not all of it is used for front-end yet */
export interface RSService extends RSSettings {
    name: string;
    organizationName: string;
    topic?: string;
    customerStatus?: string;
}

export interface RSOrganizationSettings extends RSSettings {
    description: string;
    filters: string[];
    jurisdiction: string;
    name: string;
    stateCode?: string;
    countyName?: string;
}

interface SenderKeys {
    scope: string;
    keys: object[];
}

export interface RSSender extends RSService {
    allowDuplicates: boolean;
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
}

export type RSReceiver = RSService;

export interface ApiKey {
    kty: string;
    kid: string;
    n: string;
    e: string;
}

export interface ApiKeySet {
    scope: string;
    keys: ApiKey[];
}

export interface RSApiKeysResponse {
    orgName: string;
    keys: ApiKeySet[];
}

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
    publicKeys: new RSEndpoint({
        path: ServicesUrls.PUBLIC_KEYS,
        method: HTTPMethods.GET,
        queryKey: "publicKeys",
    }),
    createPublicKey: new RSEndpoint({
        path: ServicesUrls.PUBLIC_KEYS,
        method: HTTPMethods.POST,
        queryKey: "createPublicKey",
    }),
};
