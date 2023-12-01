import { UseMutationResult, useMutation } from "@tanstack/react-query";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../../../../config/endpoints/settings";
import { useSessionContext } from "../../../../contexts/Session";
import { useAuthorizedFetch } from "../../../../contexts/AuthorizedFetch";

export interface OrganizationPublicKeyPostArgs {
    kid: string;
    sender: string;
}

export type UseCreateOrganizationPublicKeyResult = UseMutationResult<
    RSApiKeysResponse,
    unknown,
    OrganizationPublicKeyPostArgs
>;

export default function useCreateOrganizationPublicKey(): UseCreateOrganizationPublicKeyResult {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const authorizedFetch = useAuthorizedFetch<RSApiKeysResponse>();
    const mutationFn = async ({
        kid,
        sender,
    }: OrganizationPublicKeyPostArgs) => {
        return await authorizedFetch(servicesEndpoints.createPublicKey, {
            segments: { orgId: parsedName!! },
            params: {
                scope: `${parsedName}.*.report`,
                kid: `${parsedName}.${sender}`,
            },
            data: kid,
        });
    };
    return useMutation({
        mutationFn,
    });
}
