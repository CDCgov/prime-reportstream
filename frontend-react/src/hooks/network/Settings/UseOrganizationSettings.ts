import { useCallback } from "react";

import {
    servicesEndpoints,
    RSOrganizationSettings,
} from "../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../../../contexts/SessionContext";
import { Organizations } from "../../UseAdminSafeOrganizationName";

const { organization } = servicesEndpoints;

export const useOrganizationSettings = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSOrganizationSettings>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(organization, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch]
    );
    return rsUseQuery(
        [organization.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
        }
    );
};
