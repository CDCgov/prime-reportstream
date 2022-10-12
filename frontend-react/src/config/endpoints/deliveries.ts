import { HTTPMethods, RSApiEndpoints, RSEndpoint } from ".";

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
