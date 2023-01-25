import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

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
