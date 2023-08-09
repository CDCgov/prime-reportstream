import { useCallback } from "react";
import { UseQueryResult } from "@tanstack/react-query";

import { useSessionContext } from "../contexts/SessionContext";
import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";

const { senderDetail } = servicesEndpoints;

export type UseSenderResourceHookResult = UseQueryResult<RSSender>;

export default function useSenderResource(initialData?: any) {
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
        ],
    );
    return rsUseQuery(
        [senderDetail.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
            ...(initialData && { initialData: initialData }),
        },
    );
}
