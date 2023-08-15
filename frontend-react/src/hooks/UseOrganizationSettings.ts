import { useCallback } from "react";

import {
    RSOrganizationSettings,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

import { Organizations } from "./UseAdminSafeOrganizationName";

const { settings } = servicesEndpoints;

export const useOrganizationSettings = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSOrganizationSettings>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settings, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch],
    );
    return rsUseQuery(
        [settings.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
        },
    );
};
