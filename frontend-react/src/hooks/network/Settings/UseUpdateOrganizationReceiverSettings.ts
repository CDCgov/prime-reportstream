import { useCallback } from "react";

import {
    RSReceiver,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseUpdateOrganizationReceiverSettingsMutationProps {
    orgName: string;
    settings: RSReceiver;
}

export const useUpdateOrganizationReceiverSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSReceiver,
        unknown,
        UseUpdateOrganizationReceiverSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseUpdateOrganizationReceiverSettingsMutationProps) => {
            const res = await authorizedFetch(
                settingsEndpoints.updateReceiver,
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
        [settingsEndpoints.updateReceiver.queryKey],
        mutationFn
    );
};
