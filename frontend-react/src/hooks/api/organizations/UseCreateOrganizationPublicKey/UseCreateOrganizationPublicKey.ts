import { useMutation, UseMutationResult } from "@tanstack/react-query";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

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
    const { activeMembership, authorizedFetch } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const mutationFn = async ({
        kid,
        sender,
    }: OrganizationPublicKeyPostArgs) => {
        return await authorizedFetch<RSApiKeysResponse>(
            {
                segments: { orgName: parsedName! },
                params: {
                    scope: `${parsedName}.*.report`,
                    kid: `${parsedName}.${sender}`,
                },
                data: kid,
            },
            servicesEndpoints.createPublicKey,
        );
    };
    return useMutation({
        mutationFn,
    });
}
