import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

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
