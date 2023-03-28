import { useCallback } from "react";

import {
    RSOrganizationSettings,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseDeleteOrganizationSettingsMutationProps {
    orgName: string;
    settings: RSOrganizationSettings;
}

export const useDeleteOrganizationSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSOrganizationSettings,
        unknown,
        UseDeleteOrganizationSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseDeleteOrganizationSettingsMutationProps) => {
            const res = await authorizedFetch(
                settingsEndpoints.deleteOrganization,
                {
                    segments: {
                        orgName,
                        service: settings.name,
                    },
                    data: settings,
                }
            );
            return res;
        },
        [authorizedFetch]
    );

    return rsUseMutation(
        [settingsEndpoints.deleteOrganization.queryKey],
        mutationFn
    );
};
