import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import {
    messageTrackerEndpoints,
    RSMessageDetail,
} from "../../../../config/endpoints/messageTracker";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { getMessageDetails } = messageTrackerEndpoints;

/** Hook consumes the MessagesApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the covid_results_metadata_id to query a single message
 * */
const useMessageDetails = (id: string) => {
    const { authorizedFetch } = useSessionContext();

    const memoizedDataFetch = useCallback(() => {
        if (id) {
            return authorizedFetch<RSMessageDetail>(
                {
                    segments: {
                        id: id,
                    },
                },
                getMessageDetails,
            );
        }
        return null;
    }, [authorizedFetch, id]);
    const { data } = useSuspenseQuery({
        queryKey: [getMessageDetails.queryKey, id],
        queryFn: memoizedDataFetch,
    });
    return { messageDetails: data };
};

export default useMessageDetails;
