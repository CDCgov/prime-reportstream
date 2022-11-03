import { useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import {
    MessageListResource,
    messageTrackerEndpoints,
} from "../../../config/endpoints/messageTracker";

const { search } = messageTrackerEndpoints;

/** Hook consumes the MessageTrackerApi "search" endpoint and delivers the response **/
export const useMessageSearch = () => {
    const { authorizedFetch } = useAuthorizedFetch<MessageListResource[]>();

    const messagesSearch = (
        messageId: string
    ): Promise<MessageListResource[]> => {
        return authorizedFetch(search, {
            segments: { messageId },
            params: { messageId },
        });
    };

    const mutation = useMutation(messagesSearch);
    return {
        search: mutation.mutateAsync,
        isLoading: mutation.isLoading,
        error: mutation.error,
    };
};
