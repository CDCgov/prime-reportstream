import { useCallback } from "react";

import { RSSender, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

const { senders } = servicesEndpoints;

export const UseOrganizationSenders = () => {
    const { activeMembership } = useSessionContext();

    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSSender[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senders, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch]
    );
    const { data, isLoading, isInitialLoading } = rsUseQuery(
        [senders.queryKey, activeMembership],
        memoizedDataFetch
        // {
        //     enabled:
        // !!activeMembership?.parsedName && !!activeMembership.service,
        // }
    );

    return { senders: data, sendersIsLoading: isLoading, isInitialLoading };
};
