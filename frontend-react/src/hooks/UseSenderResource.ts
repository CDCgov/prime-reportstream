import { useCallback } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";

const { senderDetail } = servicesEndpoints;
export const useSenderResource = () => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSSender>();
    /* Access the session. */
    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senderDetail, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                    sender: activeMembership?.service!!,
                },
            }),
        [
            activeMembership?.parsedName,
            activeMembership?.service,
            authorizedFetch,
        ]
    );
    const { data, isLoading } = rsUseQuery(
        [senderDetail.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
        }
    );
    return { senderDetail: data, senderIsLoading: isLoading };
};
