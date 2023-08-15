import { useCallback } from "react";
import { UseQueryResult } from "@tanstack/react-query";

import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

const { senders } = servicesEndpoints;

export type UseOrganizationSendersResult = UseQueryResult<RSSender[]>;

export default function useOrganizationSenders() {
    const { activeMembership } = useSessionContext();

    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSSender[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senders, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch],
    );
    return rsUseQuery([senders.queryKey, activeMembership], memoizedDataFetch, {
        enabled: !!activeMembership?.parsedName && !!activeMembership.service,
    });
}
