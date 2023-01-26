import { useCallback } from "react";

import { settingsEndpoints } from "../config/api/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

export const useOrganizationSettings = () => {
    const { activeMembership } = useSessionContext();
    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSOrganizationSettings>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settingsEndpoints.organization, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [settingsEndpoints.organization.meta.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled: !!activeMembership?.parsedName,
        }
    );
};
