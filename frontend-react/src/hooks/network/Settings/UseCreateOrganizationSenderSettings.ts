import { useCallback } from "react";

import {
    RSSender,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseCreateOrganizationSenderSettingsMutationProps {
    orgName: string;
    settings: RSSender;
}

export const useCreateOrganizationSenderSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSSender,
        unknown,
        UseCreateOrganizationSenderSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseCreateOrganizationSenderSettingsMutationProps) => {
            const res = await authorizedFetch(settingsEndpoints.createSender, {
                segments: {
                    orgName,
                    service: settings.name,
                },
                data: settings,
            });
            return res;
        },
        [authorizedFetch]
    );

    return rsUseMutation([settingsEndpoints.createSender.queryKey], mutationFn);
};
