import { useCallback } from "react";
import { UseQueryResult, useQuery } from "@tanstack/react-query";

import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";

const { senders } = servicesEndpoints;

export type UseOrganizationSendersResult = UseQueryResult<RSSender[]>;

export default function useOrganizationSenders() {
    const { activeMembership } = useSessionContext();

    const authorizedFetch = useAuthorizedFetch<RSSender[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senders, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch],
    );
    return useQuery({
        queryKey: [senders.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
        enabled: !!activeMembership?.parsedName && !!activeMembership.service,
    });
}
