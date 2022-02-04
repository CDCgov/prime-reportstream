export interface SubmissionDetailsParams {
    actionId: number;
}

export interface Destination {
    organization_id: string;
    organization: string;
    service: string;
    filteredReportRows: string[];
    sending_at: string;
    itemCount: number;
    sentReports: string[];
}

export interface SubmissionDate {
    dateString: string;
    timeString: string;
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
