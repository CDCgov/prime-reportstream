import { useCallback } from "react";

import { settingsEndpoints } from "../../../config/api/settings";
import { useRSMutation } from "../UseRSQuery";

export interface UseCreateOrganizationSettingsMutationProps {
    orgName: string;
    settings: RSOrganizationSettings;
}

export const useCreateOrganizationSettings = () => {
    const mutationOptionsFn = useCallback(
        ({ orgName, settings }: UseCreateOrganizationSettingsMutationProps) => {
            return {
                segments: {
                    orgName,
                },
                data: settings,
            };
        },
        []
    );

    return useRSMutation(
        settingsEndpoints.organization,
        "POST",
        mutationOptionsFn
    );
};
