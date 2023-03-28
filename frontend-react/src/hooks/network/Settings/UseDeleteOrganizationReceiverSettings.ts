import { useCallback } from "react";

import {
    RSReceiver,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseDeleteOrganizationReceiverSettingsMutationProps {
    orgName: string;
    settings: RSReceiver;
}

export const useDeleteOrganizationReceiverSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSReceiver,
        unknown,
        UseDeleteOrganizationReceiverSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseDeleteOrganizationReceiverSettingsMutationProps) => {
            const res = await authorizedFetch(
                settingsEndpoints.deleteReceiver,
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
        [settingsEndpoints.deleteReceiver.queryKey],
        mutationFn
    );
};
