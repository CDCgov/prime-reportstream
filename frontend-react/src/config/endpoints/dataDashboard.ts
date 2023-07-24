import { HTTPMethods, RSApiEndpoints, RSEndpoint } from ".";

export interface FacilityResource {
    facilityId: string | undefined;
    name: string | undefined;
    location: string | undefined;
    facilityType: string | undefined;
    reportDate: string | "";
}

export interface SenderTypeDetailResource {
    reportId: string | undefined;
    batchReadyAt: string | "";
    expires: string | "";
    total: string | undefined;
}

export interface RSReceiverDelivery {
    orderingProvider: string;
    orderingFacility: string;
    submitter: string;
    reportId: string;
    createdAt: string;
    expirationDate: string;
    testResultCount: number;
}

export interface RSReceiverDeliveryMeta {
    type: string;
    totalCount: number;
    totalFilteredCount: number;
    totalPages: number;
    nextPage: number;
    previousPage: number;
}

export interface RSReceiverSubmitterMeta {
    type: string;
    totalCount: number;
    totalFilteredCount: number;
    totalPages: number;
    nextPage: number;
    previousPage: number;
}

export interface RSSubmitter {
    id: string;
    name: string;
    firstReportDate: string;
    testResultCount: number;
    type: string;
    location: string;
}

export interface RSReceiverDeliveryResponse {
    meta: RSReceiverDeliveryMeta;
    data: RSReceiverDelivery[];
}

export interface RSReceiverSubmitterResponse {
    meta: RSReceiverSubmitterMeta;
    data: RSSubmitter[];
}

/*
Deliveries API Endpoints

* receiverDeliveries -> Retrieves a list of reports for receiver by receiverFullName/orgAndService (ex: xx-phd.elr)
* receiverSubmitters -> Retrieves a list of all the providers, facilities and senders that have sent results to a receiver
*/
export const dataDashboardEndpoints: RSApiEndpoints = {
    receiverDeliveries: new RSEndpoint({
        path: "/v1/receivers/:orgAndService/deliveries",
        method: HTTPMethods.POST,
        queryKey: "deliveriesForReceiver",
    }),
    receiverSubmitters: new RSEndpoint({
        path: "/v1/receivers/:orgAndService/deliveries/submitters/search",
        method: HTTPMethods.POST,
        queryKey: "submittersForReceiver",
    }),
};
