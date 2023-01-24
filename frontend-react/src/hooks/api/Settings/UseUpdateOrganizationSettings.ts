import { useCallback } from "react";

import {
    RSOrganizationSettings,
    settingsEndpoints,
} from "../../../config/api/settings";
import { useRSMutation } from "../UseRSQuery";

export const useUpdateOrganizationSettings = (orgName: string) => {
    const mutationFunction = useCallback(
        (settings: RSOrganizationSettings) => {
            return {
                segments: {
                    orgName,
                },
                data: settings,
            };
        },
        [orgName]
    );

    return useRSMutation(
        settingsEndpoints.organization,
        "POST",
        mutationFunction
    );
};
