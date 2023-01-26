import { useMutation } from "@tanstack/react-query";
import { useCallback } from "react";

import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { messageTrackerEndpoints } from "../../../config/api/messageTracker";

const { search, getMessageDetails } = messageTrackerEndpoints;

/** Hook consumes the MessageTrackerApi "search" endpoint and delivers the response **/
export const useMessageSearch = () => {
    const { authorizedFetch } = useAuthorizedFetch<MessagesItem[]>();

    const messagesSearch = (messageId: string): Promise<MessagesItem[]> => {
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

/** Hook consumes the MessagesApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the covid_results_metadata_id to query a single message
 * */
export const useMessageDetails = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<Message>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getMessageDetails, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id]
    );
    const { data } = rsUseQuery(
        [messageTrackerEndpoints.getMessageDetails.meta.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { messageDetails: data };
};
