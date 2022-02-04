export interface Destination {
    orgId: string;
    organization: string;
    service: string;
    filteredReportRows: string[];
    sendingAt: string;
    itemCount: number;
    sentReports: string[];
}

export interface ReportWarning {
    scope: string;
    type: string;
    message: string;
}

export interface ReportError extends ReportWarning {
    index: number;
    trackingId: string;
}
