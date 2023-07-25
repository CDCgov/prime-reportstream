import { HTTPMethods, RSApiEndpoints, RSEndpoint } from ".";

// TODO: move to /resources/ once we know the data structure being returned from the API
export interface FacilityResource {
    facilityId: string | undefined;
    name: string | undefined;
    location: string | undefined;
    facilityType: string | undefined;
    reportDate: string | "";
}

// TODO: move to /resources/ once we know the data structure being returned from the API
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
}

export interface RSReceiverDeliveryResponse {
    meta: RSReceiverDeliveryMeta;
    data: RSReceiverDelivery[];
}

/*
Deliveries API Endpoints

* receiverDeliveries -> Retrieves a list of reports for receiver by receiverFullName/orgAndService (ex: xx-phd.elr)
*/
export const dataDashboardEndpoints: RSApiEndpoints = {
    receiverDeliveries: new RSEndpoint({
        path: "/v1/receivers/:orgAndService/deliveries",
        method: HTTPMethods.POST,
        queryKey: "deliveriesForReceiver",
    }),
};
