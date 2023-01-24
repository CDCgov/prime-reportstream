import { AdminAction } from "./admin";
import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

export interface HistorySummaryTest {
    id: string;
    title: string;
    subtitle: string;
    daily: number;
    last: number;
    positive: boolean;
    change: number;
    data: number[];
}

export interface HistoryReportFacility {
    facility: string | undefined;
    location: string | undefined;
    CLIA: string | undefined;
    positive: string | undefined;
    total: string | undefined;
}

export interface HistoryReport {
    sent: number;
    via: string;
    // positive: number = 1;
    total: number;
    fileType: string;
    type: string;
    reportId: string;
    expires: number;
    sendingOrg: string;
    receivingOrg: string;
    receivingOrgSvc: string;
    facilities: string[];
    actions: AdminAction[];
    displayName: string;
    content: string;
    fileName: string;
    mimeType: string;
}
/**
 * @todo Remove once refactored out of Report download call (when RSDelivery.content exists)
 * @deprecated For compile-time type checks while #5892 is worked on
 */
export interface RSReportInterface {
    batchReadyAt: number;
    via: string;
    positive: number;
    reportItemCount: number;
    fileType: string;
    type: string;
    reportId: string;
    expires: number;
    sendingOrg: string;
    receivingOrg: string;
    receivingOrgSvc: string;
    facilities: string[];
    actions: AdminAction[];
    displayName: string;
    content: string;
    fileName: string;
    mimeType: string;
}

export interface TestCard {
    id: string;
    title: string;
    subtitle: string;
    daily: number;
    last: number;
    positive: boolean;
    change: number;
    data: number[];
}

export const historyEndpoints = {
    summaryTests: new RSEndpoint({
        path: "history/summary/tests",
        methods: {
            [HTTPMethods.GET]: {} as HistorySummaryTest,
        },
        queryKey: "historySummaryTests",
    } as const),
    reportFacilities: new RSEndpoint({
        path: "history/report/:reportId/facilities",
        methods: {
            [HTTPMethods.GET]: {} as HistoryReportFacility,
        },
        queryKey: "historyReportFacilities",
    } as const),
    report: new RSEndpoint({
        path: "history/report/:reportId",
        methods: {
            [HTTPMethods.GET]: {} as HistoryReport,
        },
        queryKey: "historyReport",
    } as const),
};
