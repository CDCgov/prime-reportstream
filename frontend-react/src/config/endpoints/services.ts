import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export enum ServicesUrls {
    SENDERS = "/settings/organizations/:orgName/senders",
    RECEIVERS = "/settings/organizations/:orgName/receivers",
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
