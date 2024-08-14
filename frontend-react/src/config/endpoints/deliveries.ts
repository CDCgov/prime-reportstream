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

export interface RSFacility {
    facility: string | undefined;
    location: string | undefined;
    CLIA: string | undefined;
    positive: number | undefined;
    total: number | undefined;
}

export interface RSDeliveryHistoryMeta {
    totalCount: number; // The total number of results before applying the filters
    totalFilteredCount: number; // The total number of results after applying the filters
    totalPages: number; // The number of pages for the results
    nextPage: number; // The next page of results (optional)
    previousPage: number; // The previous page of results (optional)
}

export interface RSDeliveryHistory {
    deliveryId: string; // The delivery ID in the report
    createdAt: string; // When the report was sent, in date-time format
    expiresAt: string; // When the report will no longer be available, in date-time format
    receiver: string; // The name of the organization receiving the report and the service used by the organization
    receivingOrgSvcStatus: string; // Customer status of the service used by the organization
    reportId: string; // The ID for the sent report, in UUID format
    topic: string; // The schema topic (e.g., COVID-19, Flu)
    reportItemCount: string; // The total number of reports sent by the submitter
    fileName: string; // The filename for the delivered report
    fileType: string; // The format in which the report was originally sent
}

export interface RSDeliveryHistoryResponse {
    meta: RSDeliveryHistoryMeta;
    data: RSDeliveryHistory[];
}

/*
Deliveries API Endpoints

* getDeliveriesHistory -> Retrieves a filterable list of reports using orgAndService (ex: xx-phd.elr) or individual report by fileName or reportId
* getOrgDeliveries -> Retrieves a list of reports using orgAndService (ex: xx-phd.elr)
* getDeliveryDetails -> Retrieves details of a single report using a report id
* getDeliveryFacilities -> Retrieves a list of facilities who contributed to a report by a report id
*/
export const deliveriesEndpoints: RSApiEndpoints = {
    getDeliveriesHistory: new RSEndpoint({
        path: "/v1/waters/org/:orgAndService/deliveries",
        method: HTTPMethods.POST,
        queryKey: "getDeliveriesHistory",
    }),
    getOrgDeliveries: new RSEndpoint({
        path: "/waters/org/:orgAndService/deliveries",
        method: HTTPMethods.GET,
        queryKey: "getOrgDeliveries",
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
