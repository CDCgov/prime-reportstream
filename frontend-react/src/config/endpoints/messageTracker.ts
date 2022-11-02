import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

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
