export interface QualityFilter {
    trackingId: string | null;
    detail: any;
}

export interface ResponseReceiver {
    reportId: string;
    receivingOrg: string;
    receivingOrgSvc: string;
    transportResult: string | null;
    fileName: string | null;
    fileUrl: string | null;
    createdAt: string;
    qualityFilters: QualityFilter[];
}

export interface Warnings {
    field: string | undefined;
    description: string | undefined;
    type: string | undefined;
    trackingIds: string[] | undefined;
}
