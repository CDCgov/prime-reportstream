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

/*
Deliveries API Endpoints

* getOrgDeliveries -> Retrieves a list of reports using orgAndService (ex: xx-phd.elr)
* getDeliveryDetails -> Retrieves details of a single report using a report id
* getDeliveryFacilities -> Retrieves a list of facilities who contributed to a report by a report id
*/
export const deliveriesEndpoints: RSApiEndpoints = {
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
