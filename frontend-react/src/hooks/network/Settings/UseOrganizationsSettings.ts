import { useCallback } from "react";

import {
    settingsEndpoints,
    RSOrganizationSettings,
} from "../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../../../contexts/SessionContext";
import { Organizations } from "../../UseAdminSafeOrganizationName";

const { organizations } = settingsEndpoints;

export const useOrganizationsSettings = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSOrganizationSettings[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(organizations, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [organizations.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
        }
    );
};
