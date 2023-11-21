import { useCallback } from "react";
import { UseQueryResult, useQuery } from "@tanstack/react-query";

import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";

const { senders } = servicesEndpoints;

export type UseOrganizationSendersResult = UseQueryResult<RSSender[]>;

export default function useOrganizationSenders(orgName?: string) {
    const { activeMembership } = useSessionContext();

    const authorizedFetch = useAuthorizedFetch<RSSender[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senders, {
                segments: {
                    orgName: orgName ?? activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch, orgName],
    );
    return useQuery({
        queryKey: [senders.queryKey, orgName ?? activeMembership],
        queryFn: memoizedDataFetch,
        enabled:
            !!orgName ||
            (!!activeMembership?.parsedName && !!activeMembership.service),
    });
}
