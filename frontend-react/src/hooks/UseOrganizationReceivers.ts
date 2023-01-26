import { useCallback } from "react";

import { settingsEndpoints } from "../config/api/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

import { Organizations } from "./UseAdminSafeOrganizationName";

export const useOrganizationReceivers = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSReceiver[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settingsEndpoints.receivers, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [settingsEndpoints.receivers.meta.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
        }
    );
};
