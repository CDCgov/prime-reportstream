import { useCallback } from "react";
import { UseQueryResult, useQuery } from "@tanstack/react-query";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../../contexts/AuthorizedFetch";
import { useSessionContext } from "../../../../contexts/Session";

const { publicKeys } = servicesEndpoints;

export type UseOrganizationPublicKeysResult = UseQueryResult<RSApiKeysResponse>;

export default function useOrganizationPublicKeys(orgName?: string) {
    const authorizedFetch = useAuthorizedFetch<RSApiKeysResponse>();

    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(publicKeys, {
                segments: {
                    orgName: orgName ?? activeMembership?.parsedName!!,
                },
            }),
        [activeMembership?.parsedName, authorizedFetch, orgName],
    );
    return useQuery({
        queryKey: [publicKeys.queryKey, orgName ?? activeMembership],
        queryFn: memoizedDataFetch,
        enabled:
            !!orgName ||
            (!!activeMembership?.parsedName && !!activeMembership.service),
    });
}
