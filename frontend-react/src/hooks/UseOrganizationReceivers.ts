import { useCallback } from "react";

import { settingsEndpoints } from "../config/api/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

export const useOrganizationReceivers = () => {
    const { activeMembership } = useSessionContext();
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSReceiver[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settingsEndpoints.receivers, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [settingsEndpoints.receivers.meta.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled: !!activeMembership?.parsedName,
        }
    );
};
