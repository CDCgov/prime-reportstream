import { useCallback } from "react";
import { useSuspenseQuery } from "@tanstack/react-query";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../../contexts/AuthorizedFetch";
import { useSessionContext } from "../../../../contexts/Session";

const { publicKeys } = servicesEndpoints;

export type UseOrganizationPublicKeysResult = ReturnType<
    typeof useOrganizationPublicKeys
>;

export default function useOrganizationPublicKeys() {
    const authorizedFetch = useAuthorizedFetch<RSApiKeysResponse>();

    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(() => {
        if (activeMembership?.parsedName) {
            return authorizedFetch(publicKeys, {
                segments: {
                    orgName: activeMembership.parsedName,
                },
            });
        }
        return null;
    }, [activeMembership?.parsedName, authorizedFetch]);
    return useSuspenseQuery({
        queryKey: [publicKeys.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });
}
