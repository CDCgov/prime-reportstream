import { useCallback } from "react";

import {
    RSSender,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseUpdateOrganizationSenderSettingsMutationProps {
    orgName: string;
    settings: RSSender;
}

export const useUpdateOrganizationSenderSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSSender,
        unknown,
        UseUpdateOrganizationSenderSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseUpdateOrganizationSenderSettingsMutationProps) => {
            const res = await authorizedFetch(settingsEndpoints.updateSender, {
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

    return rsUseMutation([settingsEndpoints.updateSender.queryKey], mutationFn);
};
