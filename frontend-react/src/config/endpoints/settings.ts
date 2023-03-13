import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export enum ServicesUrls {
    ORGANIZATIONS = "/settings/organizations",
    ORGANIZATION = "/settings/organizations/:orgName",
    SENDERS = "/settings/organizations/:orgName/senders",
    SENDER = "/settings/organizations/:orgName/senders/:sender",
    RECEIVERS = "/settings/organizations/:orgName/receivers",
    RECEIVER = "/settings/organizations/:orgName/receivers/:receiver",
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
    organizations: new RSEndpoint({
        path: ServicesUrls.ORGANIZATION,
        method: HTTPMethods.GET,
        queryKey: "settingsOrganizations",
    }),
    organization: new RSEndpoint({
        path: ServicesUrls.ORGANIZATION,
        method: HTTPMethods.GET,
        queryKey: "settingsOrganization",
    }),
    senders: new RSEndpoint({
        path: ServicesUrls.SENDERS,
        method: HTTPMethods.GET,
        queryKey: "settingsOrganizationSenders",
    }),
    sender: new RSEndpoint({
        path: ServicesUrls.SENDER,
        method: HTTPMethods.GET,
        queryKey: "settingsOrganizationSender",
    }),
    receivers: new RSEndpoint({
        path: ServicesUrls.RECEIVERS,
        method: HTTPMethods.GET,
        queryKey: "settingsOrganizationReceivers",
    }),
    receiver: new RSEndpoint({
        path: ServicesUrls.RECEIVER,
        method: HTTPMethods.GET,
        queryKey: "settingsOrganizationReceiver",
    }),
};
