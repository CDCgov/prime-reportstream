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

/*
Deliveries API Endpoints

* getOrgDeliveries -> Retrieves a list of reports using orgAndService (ex: xx-phd.elr)
* getReportDetails -> Get the report details for a report
* getPerformingFacilities -> Retrieves a list of facilities and providers
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
    getFacilitiesAndProviders: new RSEndpoint({
        path: "/waters/report/:id/facilities",
        method: HTTPMethods.GET,
        queryKey: "getDeliveryFacilities",
    }),
};
