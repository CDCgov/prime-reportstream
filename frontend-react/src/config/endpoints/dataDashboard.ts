import { HTTPMethods, RSApiEndpoints, RSEndpoint } from ".";

// TODO: will need to be revisited once new API is ready
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
export interface RSFacilityProvider {
    provider: string | undefined;
    facility: string | undefined;
    location: string | undefined;
    total: number | undefined;
    collectionDate: string | undefined;
}

/*
Deliveries API Endpoints

* getOrgDeliveries -> Retrieves a list of reports using orgAndService (ex: xx-phd.elr)
*/

// TODO: will need to be revisited once new API's are ready
export const dataDashboardEndpoints: RSApiEndpoints = {
    getOrgDeliveries: new RSEndpoint({
        path: "/waters/org/:orgAndService/deliveries",
        method: HTTPMethods.GET,
        queryKey: "getOrgDeliveries",
    }),
    getReportDetails: new RSEndpoint({
        path: "/waters/report/:id/delivery",
        method: HTTPMethods.GET,
        queryKey: "getDeliveryDetails",
    }),
    getPerformingFacilities: new RSEndpoint({
        path: "/waters/report/:id/facilities",
        method: HTTPMethods.GET,
        queryKey: "getDeliveryFacilities",
    }),
};
