import { useCallback } from "react";

import {
    Message,
    messageTrackerEndpoints,
} from "../../../config/api/messageTracker";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";

// TODO: Use commented out code instead
/** Hook consumes the MessagesApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the covid_results_metadata_id to query a single message
 * */
export const useMessageDetails = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<Message>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(messageTrackerEndpoints.getMessageDetails, {
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

// /** Hook consumes the MessagesApi "detail" endpoint and delivers the response
//  *
//  * @param id {string} Pass in the covid_results_metadata_id to query a single message
//  * */
// export const useMessageDetails = (id: string) => {
//     return useRSQuery(messageTrackerEndpoints.getMessageDetails, {
//         segments: {
//             id,
//         },
//     });
// };
