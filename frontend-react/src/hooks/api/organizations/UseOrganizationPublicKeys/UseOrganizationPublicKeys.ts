import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { publicKeys } = servicesEndpoints;

export type UseOrganizationPublicKeysResult = ReturnType<
    typeof useOrganizationPublicKeys
>;

export default function useOrganizationPublicKeys() {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const memoizedDataFetch = useCallback(() => {
        if (activeMembership?.parsedName) {
            return authorizedFetch<RSApiKeysResponse>(
                {
                    segments: {
                        orgName: activeMembership.parsedName,
                    },
                },
                publicKeys,
            );
        }
        return null;
    }, [activeMembership?.parsedName, authorizedFetch]);
    return useSuspenseQuery({
        queryKey: [publicKeys.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });
}
