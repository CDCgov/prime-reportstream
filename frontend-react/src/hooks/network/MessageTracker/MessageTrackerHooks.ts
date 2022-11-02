import { useCallback } from "react";

import {
    RSMessageDetail,
    messageTrackerEndpoints,
} from "../../../config/endpoints/messageTracker";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";

const { getMessageDetails } = messageTrackerEndpoints;

/** Hook consumes the MessagesApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the covid_results_metadata_id to query a single message
 * */
const useMessageDetails = (id: string) => {
    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSMessageDetail>();
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
        [getMessageDetails.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { messageDetails: data };
};

export { useMessageDetails };
