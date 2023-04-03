import { useCallback } from "react";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

export interface OrganizationPublicKeyPostArgs {
    kid: string;
    sender: string;
}

export const useCreateOrganizationPublicKey = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSApiKeysResponse,
        unknown,
        OrganizationPublicKeyPostArgs
    >();
    const mutationFn = useCallback(
        async ({ kid, sender }: OrganizationPublicKeyPostArgs) => {
            const res = await authorizedFetch(
                servicesEndpoints.createPublicKey,
                {
                    segments: { organizationName: parsedName!! },
                    params: {
                        scope: `${parsedName}.*.report`,
                        kid: `${parsedName}.${sender}`,
                    },
                    data: kid,
                }
            );
            return res;
        },
        [authorizedFetch]
    );
    return rsUseMutation(
        [servicesEndpoints.createPublicKey.queryKey],
        mutationFn
    );
};
