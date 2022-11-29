import { useCallback } from "react";

import {
    RSOrganizationSettings,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

const { settings } = servicesEndpoints;

export const useOrganizationSettings = () => {
    const { activeMembership } = useSessionContext();
    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSOrganizationSettings>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settings, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [settings.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled: !!activeMembership?.parsedName,
        }
    );
};
