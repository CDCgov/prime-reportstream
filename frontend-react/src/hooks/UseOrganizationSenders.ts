import { useCallback } from "react";
import { useSuspenseQuery } from "@tanstack/react-query";

import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";

const { senders } = servicesEndpoints;

export type UseOrganizationSendersResult = ReturnType<
    typeof useOrganizationSenders
>;

export default function useOrganizationSenders() {
    const { activeMembership } = useSessionContext();

    const authorizedFetch = useAuthorizedFetch<RSSender[]>();
    const memoizedDataFetch = useCallback(() => {
        if (!!activeMembership?.parsedName) {
            return authorizedFetch(senders, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            });
        }
        return null;
    }, [activeMembership?.parsedName, authorizedFetch]);
    return useSuspenseQuery({
        queryKey: [senders.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });
}
