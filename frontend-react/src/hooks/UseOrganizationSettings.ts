import { useCallback } from "react";

import { settingsEndpoints } from "../config/api/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

import { Organizations } from "./UseAdminSafeOrganizationName";

export const useOrganizationSettings = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSOrganizationSettings>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settingsEndpoints.organization, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [settingsEndpoints.organization.meta.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
        }
    );
};
