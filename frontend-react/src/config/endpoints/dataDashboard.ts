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
}

/*
Deliveries API Endpoints

* getOrgDeliveries -> Retrieves a list of reports using orgAndService (ex: xx-phd.elr)
*/

// TODO: will need to be revisited once new API is ready
export const dataDashboardEndpoints: RSApiEndpoints = {
    getOrgDeliveries: new RSEndpoint({
        path: "/waters/org/:orgAndService/deliveries",
        method: HTTPMethods.GET,
        queryKey: "getOrgDeliveries",
    }),
};
