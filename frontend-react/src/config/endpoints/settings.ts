import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export enum ServicesUrls {
    SETTINGS = "/settings/organizations/:orgId",
    SENDERS = "/settings/organizations/:orgId/senders",
    SENDER_DETAIL = "/settings/organizations/:orgId/senders/:senderId",
    RECEIVERS = "/settings/organizations/:orgId/receivers",
    PUBLIC_KEYS = "/settings/organizations/:orgId/public-keys",
}

/** Response is much larger than this but not all of it is used for front-end yet */
export interface RSService extends RSSetting {
    customerStatus?: string;
}

export interface RSSetting {
    createdAt?: string;
    createdBy?: string;
    name: string;
    organizationName: string;
    topic: string;
    customerStatus?: string;
    version: number;
}

export interface RSOrganizationSettings extends RSSetting {
    description: string;
    filters: string[];
    jurisdiction: string;
    name: string;
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
    customerStatus: "inactive" | "testing" | "active";
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

export interface RSReceiver extends RSService {
    customerStatus: string;
    translation: any;
    jurisdictionalFilter?: object;
    qualityFilter?: object;
    routingFilter?: object;
    processingModeFilter?: object;
    reverseTheQualityFilter?: boolean;
    conditionFilter?: object;
    deidentify?: boolean;
    deidentifiedValue?: string;
    timing?: object;
    timeZone?: string;
    dateTimeFormat?: string;
    description?: string;
    transport?: object;
    externalName?: string;
}

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
