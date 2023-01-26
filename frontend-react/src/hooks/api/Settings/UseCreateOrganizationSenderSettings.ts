import { useCallback } from "react";

import { settingsEndpoints } from "../../../config/api/settings";
import { useRSMutation } from "../UseRSQuery";

export const useCreateOrganizationSenderSettings = () => {
    const mutationOptionsFn = useCallback(
        (settings: RSOrganizationSettings) => {
            return {
                segments: {
                    orgName: settings.organizationName,
                    sender: settings.name,
                },
                data: settings,
            };
        },
        []
    );

    return useRSMutation(settingsEndpoints.sender, "POST", mutationOptionsFn);
};
