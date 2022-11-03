import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

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
    class: string | null;
    fieldMapping: string | null;
    scope: string | null;
    message: string | null;
}

export interface MessageListResource {
    messageId: string;
    sender: string | undefined;
    submittedDate: string | undefined;
    reportId: string;
}

export const messageTrackerEndpoints: RSApiEndpoints = {
    search: new RSEndpoint({
        path: "/messages/search",
        method: HTTPMethods.GET,
        queryKey: "messagesSearch",
    }),
};
