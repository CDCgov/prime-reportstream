import { useCallback } from "react";
import { UseMutationResult } from "@tanstack/react-query";

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

export type UseCreateOrganizationPublicKeyResult = UseMutationResult<
    RSApiKeysResponse,
    unknown,
    OrganizationPublicKeyPostArgs
>;

export const useCreateOrganizationPublicKey =
    (): UseCreateOrganizationPublicKeyResult => {
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
                        segments: { orgName: parsedName!! },
                        params: {
                            scope: `${parsedName}.*.report`,
                            kid: `${parsedName}.${sender}`,
                        },
                        data: kid,
                    }
                );
                return res;
            },
            [authorizedFetch, parsedName]
        );
        return rsUseMutation(
            [servicesEndpoints.createPublicKey.queryKey],
            mutationFn
        );
    };
