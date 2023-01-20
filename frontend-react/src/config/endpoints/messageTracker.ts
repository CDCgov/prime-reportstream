import { HTTPMethods, RSEndpoints, RSEndpoint } from "./RSEndpoint";

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
    messageId: string;
    sender: string | undefined;
    submittedDate: string | undefined;
    reportId: string;
}

export const messageTrackerEndpoints: RSEndpoints = {
    search: new RSEndpoint({
        path: "/messages/search",
        methods: {
            [HTTPMethods.GET]: {} as unknown,
        },
        queryKey: "messagesSearch",
    }),
    getMessageDetails: new RSEndpoint({
        path: "/message/:id",
        methods: {
            [HTTPMethods.GET]: {} as unknown,
        },
        queryKey: "messageDetails",
    }),
};
