import { useCallback } from "react";
import { useQuery } from "@tanstack/react-query";

import { useSessionContext } from "../contexts/Session";
import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";

const { senderDetail } = servicesEndpoints;

export type UseSenderResourceHookResult = ReturnType<typeof useSenderResource>;

export default function useSenderResource(initialData?: RSSender) {
    const authorizedFetch = useAuthorizedFetch<RSSender>();
    /* Access the session. */
    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senderDetail, {
                segments: {
                    orgId: activeMembership?.parsedName!!,
                    entityId: activeMembership?.service!!,
                },
            }),
        [
            activeMembership?.parsedName,
            activeMembership?.service,
            authorizedFetch,
        ],
    );
    return useQuery({
        queryKey: [senderDetail.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
        enabled: !!activeMembership?.parsedName && !!activeMembership.service,
        initialData: initialData,
    });
}
