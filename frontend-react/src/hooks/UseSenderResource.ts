import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";

const { senderDetail } = servicesEndpoints;

export type UseSenderResourceHookResult = ReturnType<typeof useSenderResource>;

export default function useSenderResource(initialData?: RSSender) {
    const authorizedFetch = useAuthorizedFetch<RSSender>();
    /* Access the session. */
    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(() => {
        if (!!activeMembership?.parsedName && !!activeMembership.service) {
            return authorizedFetch(senderDetail, {
                segments: {
                    orgName: activeMembership.parsedName,
                    sender: activeMembership.service,
                },
            });
        }
        return null;
    }, [
        activeMembership?.parsedName,
        activeMembership?.service,
        authorizedFetch,
    ]);
    return useSuspenseQuery({
        queryKey: [senderDetail.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
        initialData: initialData,
    });
}
