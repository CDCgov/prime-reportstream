import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

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

export interface MessagesItem {
    messageId: string;
    sender: string | undefined;
    submittedDate: string | undefined;
    reportId: string;
}

export interface Message extends MessagesItem {
    id: number;
    fileName: string | null;
    fileUrl: string | null;
    warnings: WarningError[];
    errors: WarningError[];
    receiverData: ReceiverData[];
}

export const messageTrackerEndpoints = {
    search: new RSEndpoint({
        path: "/messages/search",
        methods: {
            [HTTPMethods.GET]: {} as MessagesItem[],
        },
        queryKey: "messagesSearch",
    } as const),
    getMessageDetails: new RSEndpoint({
        path: "/message/:id",
        methods: {
            [HTTPMethods.GET]: {} as Message,
        },
        queryKey: "messageDetails",
    } as const),
};
