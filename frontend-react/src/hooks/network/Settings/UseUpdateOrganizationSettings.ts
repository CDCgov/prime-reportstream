import { useCallback } from "react";

import {
    RSOrganizationSettings,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseUpdateOrganizationSettingsMutationProps {
    orgName: string;
    settings: RSOrganizationSettings;
}

export const useUpdateOrganizationSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSOrganizationSettings,
        unknown,
        UseUpdateOrganizationSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseUpdateOrganizationSettingsMutationProps) => {
            const res = await authorizedFetch(
                settingsEndpoints.updateOrganization,
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
        [settingsEndpoints.updateOrganization.queryKey],
        mutationFn
    );
};
