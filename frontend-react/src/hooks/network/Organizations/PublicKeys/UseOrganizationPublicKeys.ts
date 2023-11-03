import { useCallback } from "react";
import { UseQueryResult, useQuery } from "@tanstack/react-query";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../../../../contexts/SessionContext";

const { publicKeys } = servicesEndpoints;

export type UseOrganizationPublicKeysResult = UseQueryResult<RSApiKeysResponse>;

export default function useOrganizationPublicKeys() {
    const authorizedFetch = useAuthorizedFetch<RSApiKeysResponse>();

    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(publicKeys, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch],
    );
    return useQuery({
        queryKey: [publicKeys.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
        enabled: !!activeMembership?.parsedName && !!activeMembership.service,
    });
}
