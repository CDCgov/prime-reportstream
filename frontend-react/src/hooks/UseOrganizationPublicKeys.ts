import { useCallback } from "react";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

const { publicKeys } = servicesEndpoints;

export type useOrganizationPublicKeysResult = {
    publicKeys: RSApiKeysResponse[];
    isLoading: boolean;
};

export const UseOrganizationPublicKeys = () => {
    const { activeMembership } = useSessionContext();

    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSApiKeysResponse[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(publicKeys, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
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

    return { publicKeys: data, isLoading };
};
