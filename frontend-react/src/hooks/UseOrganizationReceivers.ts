import { useCallback } from "react";

import { RSReceiver, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

const { receivers } = servicesEndpoints;

export const useOrganizationReceivers = () => {
    const { activeMembership } = useSessionContext();
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSReceiver[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(receivers, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [receivers.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled: !!activeMembership?.parsedName,
        }
    );
};
