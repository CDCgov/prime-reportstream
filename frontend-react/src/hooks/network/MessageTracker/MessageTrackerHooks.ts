import { useMutation, useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import {
    MessageListResource,
    messageTrackerEndpoints,
    RSMessageDetail,
} from "../../../config/endpoints/messageTracker";
import useAuthorizedFetch from "../../../contexts/AuthorizedFetch/useAuthorizedFetch";

const { search, getMessageDetails } = messageTrackerEndpoints;

/** Hook consumes the MessageTrackerApi "search" endpoint and delivers the response **/
export const useMessageSearch = () => {
    const authorizedFetch = useAuthorizedFetch<MessageListResource[]>();

    const messagesSearch = (
        messageId: string,
    ): Promise<MessageListResource[]> => {
        return authorizedFetch(search, {
            segments: { messageId },
            params: { messageId },
        });
    };

    return useMutation({ mutationFn: messagesSearch });
};

/** Hook consumes the MessagesApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the covid_results_metadata_id to query a single message
 * */
export const useMessageDetails = (id: string) => {
    const authorizedFetch = useAuthorizedFetch<RSMessageDetail>();
    const memoizedDataFetch = useCallback(() => {
        if (id) {
            return authorizedFetch(getMessageDetails, {
                segments: {
                    id: id,
                },
            });
        }
        return null;
    }, [authorizedFetch, id]);
    const { data } = useSuspenseQuery({
        queryKey: [getMessageDetails.queryKey, id],
        queryFn: memoizedDataFetch,
    });
    return { messageDetails: data };
};
