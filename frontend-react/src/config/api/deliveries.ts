import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

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

export interface Destination {
    organization_id: string;
    organization: string;
    service: string;
    filteredReportRows: string[];
    filteredReportItems: FilteredReportItem[];
    sending_at: string;
    itemCount: number;
    itemCountBeforeQualityFiltering: number;
    sentReports: string[];
}

export interface FilteredReportItem {
    filterType: string;
    filterName: string;
    filteredTrackingElement: string;
    filterArgs: string[];
    message: string;
}

export interface ReportWarning {
    scope: string;
    errorCode: string;
    type: string;
    message: string;
}

export interface ReportError extends ReportWarning {
    index: number;
    trackingId: string;
}

export type OrganizationSubmissionsParams = {
    organization: string;
    sortdir: string;
    sortcol: string;
    cursor: string;
    since: string;
    until: string;
    pageSize: number;
    showFailed: boolean;
};

export interface OrganizationSubmission {
    submissionId: number;
    timestamp: string; // format is "2022-02-01T15:11:58.200754Z"
    sender: string;
    httpStatus: number;
    externalName: string;
    id: string | undefined;
    topic: string;
    reportItemCount: number;
    warningCount: number;
    errorCount: number;
}

export interface ActionDetail {
    submissionId: number;
    timestamp: string; //comes in format yyyy-mm-ddThh:mm:ss.sssZ
    sender: string | undefined;
    httpStatus: number;
    externalName: string;
    id: string;
    destinations: Destination[];
    errors: ReportError[];
    warnings: ReportWarning[];
    topic: string;
    warningCount: number;
    errorCount: number;
}

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
        path: "waters/org/:orgAndService/submissions",
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
            [HTTPMethods.GET]: {} as RSFacility,
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
