import { useCallback } from "react";

import {
    RSOrganizationSettings,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseCreateOrganizationSettingsMutationProps {
    orgName: string;
    settings: RSOrganizationSettings;
}

export const useCreateOrganizationSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSOrganizationSettings,
        unknown,
        UseCreateOrganizationSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseCreateOrganizationSettingsMutationProps) => {
            const res = await authorizedFetch(
                settingsEndpoints.createOrganization,
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
        [settingsEndpoints.createOrganization.queryKey],
        mutationFn
    );
};
