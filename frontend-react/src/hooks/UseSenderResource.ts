import { useCallback } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { settingsEndpoints } from "../config/api/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";

export const useSenderResource = () => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSSender>();
    /* Access the session. */
    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settingsEndpoints.sender, {
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
        [settingsEndpoints.sender.meta.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
        }
    );
    return { senderDetail: data, senderIsLoading: isLoading };
};
