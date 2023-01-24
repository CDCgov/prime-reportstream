import { useCallback } from "react";

import { settingsEndpoints } from "../../../config/api/settings";
import { useRSMutation } from "../UseRSQuery";

export const useDeleteOrganizationSettings = () => {
    const mutationFunction = useCallback(
        (orgName: string) => ({
            segments: {
                orgName,
            },
        }),
        []
    );

    return useRSMutation(
        settingsEndpoints.organization,
        "DELETE",
        mutationFunction
    );
};
