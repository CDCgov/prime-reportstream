import { HTTPMethods, RSApiEndpoints, RSEndpoint } from ".";

export interface RSDelivery {
    deliveryId: number;
    batchReadyAt: string;
    expires: string;
    receiver: string;
    reportId: string;
    topic: string;
    reportItemCount: number;
    fileName: string;
    fileType: string;
}

export interface Delivery {
    orderingProvider: string;
    orderingFacility: string;
    submitter: string;
    reportId: string;
    createdAt: string;
    expirationDate: string;
    testResultCount: number;
}

export interface Meta {
    type: string;
    totalCount: number;
    totalFilteredCount: number;
    totalPages: number;
    nextPage: number;
}

export interface RSApiDeliveryResponse {
    meta: Meta;
    data: Delivery[];
}

export interface RSFacility {
    facility: string | undefined;
    location: string | undefined;
    CLIA: string | undefined;
    positive: number | undefined;
    total: number | undefined;
}

/*
Deliveries API Endpoints

* receiverDeliveries -> Retrieves a list of reports for receiver by receiverFullName/orgAndService (ex: xx-phd.elr)
* getDeliveryDetails -> Retrieves details of a single report using a report id
* getDeliveryFacilities -> Retrieves a list of facilities who contributed to a report by a report id
*/
export const deliveriesEndpoints: RSApiEndpoints = {
    receiverDeliveries: new RSEndpoint({
        path: "/v1/receivers/:orgAndService/deliveries",
        method: HTTPMethods.POST,
        queryKey: "deliveriesForReceiver",
    }),
    getDeliveryDetails: new RSEndpoint({
        path: "/waters/report/:id/delivery",
        method: HTTPMethods.GET,
        queryKey: "getDeliveryDetails",
    }),
    getDeliveryFacilities: new RSEndpoint({
        path: "/waters/report/:id/facilities",
        method: HTTPMethods.GET,
        queryKey: "getDeliveryFacilities",
    }),
};
