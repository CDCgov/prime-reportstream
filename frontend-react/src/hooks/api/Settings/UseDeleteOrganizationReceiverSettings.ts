import { useCallback } from "react";

import { settingsEndpoints } from "../../../config/api/settings";
import { useRSMutation } from "../UseRSQuery";

export const useDeleteOrganizationReceiverSettings = () => {
    const mutationFunction = useCallback(
        (receiver: RSReceiver) => ({
            segments: {
                orgName: receiver.organizationName,
                receiverId: receiver.name,
            },
        }),
        []
    );

    return useRSMutation(
        settingsEndpoints.receiver,
        "DELETE",
        mutationFunction
    );
};
