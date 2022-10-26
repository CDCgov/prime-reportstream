import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export enum ServicesUrls {
    SENDERS = "/settings/organizations/:orgName/senders",
    RECEIVERS = "/settings/organizations/:orgName/receivers",
}

/** Response is much larger than this but not all of it is used for front-end yet */
export interface RSService {
    name: string;
    organizationName: string;
    topic: string;
    customerStatus: string;
}

/*
Services Endpoints
* senders -> fetches a list of organization's senders
* receivers -> fetches a list of organization's receivers
*/
export const servicesEndpoints: RSApiEndpoints = {
    senders: new RSEndpoint({
        path: ServicesUrls.SENDERS,
        method: HTTPMethods.GET,
        queryKey: "servicesSenders",
    }),
    receivers: new RSEndpoint({
        path: ServicesUrls.RECEIVERS,
        method: HTTPMethods.GET,
        queryKey: "servicesReceivers",
    }),
};
