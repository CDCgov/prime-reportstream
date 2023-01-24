import { useCallback } from "react";

import { checkSettingsCmdEndpoints } from "../../config/api/check";

import { useRSMutation } from "./UseRSQuery";

export const useCheckSettingsCmd = () => {
    const updateValueSet = useCallback(
        (orgName: string, receiverName: string) => ({
            segments: { orgName, receiverName },
        }),
        []
    );
    const mutation = useRSMutation(
        checkSettingsCmdEndpoints.checkOrganizationReceiver,
        "POST",
        updateValueSet
    );

    // TODO: Pass the whole mutation object
    return {
        doCheck: mutation.mutateAsync,
        isLoading: mutation.isLoading,
        error: mutation.error,
    };
};
