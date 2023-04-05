import { useCallback } from "react";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

const { publicKeys } = servicesEndpoints;

export const useOrganizationPublicKeys = () => {
    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSApiKeysResponse[]>();

    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(publicKeys, {
                segments: {
                    organizationName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch]
    );
    const { data, isLoading } = rsUseQuery(
        [publicKeys.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
        }
    );

    return { data, isLoading };
};
