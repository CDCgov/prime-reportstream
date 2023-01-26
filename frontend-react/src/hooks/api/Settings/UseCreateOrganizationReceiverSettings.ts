import { useCallback } from "react";

import { settingsEndpoints } from "../../../config/api/settings";
import { useRSMutation } from "../UseRSQuery";

export const useCreateOrganizationReceiverSettings = () => {
    const mutationOptionsFn = useCallback((settings: RSReceiver) => {
        return {
            segments: {
                orgName: settings.organizationName,
                receiverId: settings.name,
            },
            data: settings,
        };
    }, []);

    return useRSMutation(settingsEndpoints.receiver, "POST", mutationOptionsFn);
};
