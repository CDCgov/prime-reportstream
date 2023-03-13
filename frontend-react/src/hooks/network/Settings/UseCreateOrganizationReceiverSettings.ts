import { useCallback } from "react";

import {
    RSReceiver,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseCreateOrganizationReceiverSettingsMutationProps {
    orgName: string;
    settings: RSReceiver;
}

export const useCreateOrganizationReceiverSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSReceiver,
        unknown,
        UseCreateOrganizationReceiverSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseCreateOrganizationReceiverSettingsMutationProps) => {
            const res = await authorizedFetch(
                settingsEndpoints.createReceiver,
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
        [settingsEndpoints.createReceiver.queryKey],
        mutationFn
    );
};
