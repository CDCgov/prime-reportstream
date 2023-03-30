import { useCallback } from "react";

import {
    RSApiKeysResponse,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

export interface OrganizationPublicKeyPostArgs {
    fileName: string;
    fileContent: string;
    contentType?: string;
    format: string;
}

// export interface UseCreateOrganizationPublicKeyProps {
//     orgName: string;
//     apiKeys: RSApiKeysResponse;
// }

export const useCreateOrganizationPublicKey = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSApiKeysResponse,
        unknown,
        OrganizationPublicKeyPostArgs
    >();
    const mutationFn = useCallback(
        async ({
            fileName,
            fileContent,
            contentType,
            format,
        }: OrganizationPublicKeyPostArgs) => {
            const res = await authorizedFetch(
                servicesEndpoints.createPublicKey,
                {
                    segments: { organizationName: parsedName!! },
                    headers: {
                        "Content-Type": contentType,
                        payloadName: fileName,
                        scope: `${parsedName}.*.report`,
                    },
                    data: fileContent,
                    params: {
                        format,
                    },
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
