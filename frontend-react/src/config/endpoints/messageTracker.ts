import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

export interface QualityFilter {
    trackingId: string | null;
    detail: any;
}

export interface ReceiverData {
    reportId: string;
    receivingOrg: string;
    receivingOrgSvc: string;
    transportResult: string | null;
    fileName: string | null;
    fileUrl: string | null;
    createdAt: string;
    qualityFilters: QualityFilter[];
}

export interface WarningError {
    detail: WarningErrorDetail;
}

export interface WarningErrorDetail {
    class: string | null;
    fieldMapping: string | null;
    scope: string | null;
    message: string | null;
}

export interface RSMessageDetail {
    id: number;
    messageId: string;
    sender: string | null;
    submittedDate: string | null;
    reportId: string;
    fileName: string | null;
    fileUrl: string | null;
    warnings: WarningError[];
    errors: WarningError[];
    receiverData: ReceiverData[];
}

export interface MessageListResource {
    id: number;
    messageId: string;
    sender: string | undefined;
    submittedDate: string | undefined;
    reportId: string;
}

export const messageTrackerEndpoints: RSApiEndpoints = {
    search: new RSEndpoint({
        path: "/messages",
        method: HTTPMethods.GET,
        queryKey: "messagesSearch",
    }),
    getMessageDetails: new RSEndpoint({
        path: "/message/:id",
        method: HTTPMethods.GET,
        queryKey: "messageDetails",
    }),
};
