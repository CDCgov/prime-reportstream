import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

/*
Deliveries API Endpoints

* getOrgDeliveries -> Retrieves a list of reports using orgAndService (ex: xx-phd.elr)
* getDeliveryDetails -> Retrieves details of a single report using a report id
* getDeliveryFacilities -> Retrieves a list of facilities who contributed to a report by a report id
*/
export const deliveriesEndpoints = {
    orgAndServiceDeliveries: new RSEndpoint({
        path: "/waters/org/:orgAndService/deliveries",
        methods: {
            [HTTPMethods.GET]: {} as RSDelivery[],
        },
        queryKey: "getOrgDeliveries",
    } as const),
    orgAndServiceSubmissions: new RSEndpoint({
        path: "/waters/org/:orgAndService/submissions",
        methods: {
            [HTTPMethods.GET]: {} as OrganizationSubmission[],
        },
        queryKey: "getOrgSubmissions",
    } as const),
    reportDelivery: new RSEndpoint({
        path: "/waters/report/:id/delivery",
        methods: {
            [HTTPMethods.GET]: {} as RSDelivery,
        },
        queryKey: "getDeliveryDetails",
    } as const),
    reportFacilities: new RSEndpoint({
        path: "/waters/report/:id/facilities",
        methods: {
            [HTTPMethods.GET]: {} as RSFacility[],
        },
        queryKey: "getDeliveryFacilities",
    } as const),
    reportHistory: new RSEndpoint({
        path: "/waters/report/:id/history",
        methods: {
            [HTTPMethods.GET]: {} as unknown,
        },
        queryKey: "getDeliveryHistory",
    } as const),
};
