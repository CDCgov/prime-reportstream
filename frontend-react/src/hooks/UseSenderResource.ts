import { useCallback } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";

const { senderDetail } = servicesEndpoints;

export type UseSenderResourceHookResult = {
    senderDetail?: RSSender;
    senderIsLoading: boolean;
    isInitialLoading: boolean;
};

export const useSenderResource = (
    initialData?: any
): UseSenderResourceHookResult => {
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
    const { data, isLoading, isInitialLoading } = rsUseQuery(
        [senderDetail.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
            ...(initialData && { initialData: initialData }),
        }
    );

    return { senderDetail: data, senderIsLoading: isLoading, isInitialLoading };
};
