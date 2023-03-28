import { useCallback } from "react";

import {
    RSSender,
    settingsEndpoints,
} from "../../../config/endpoints/settings";
import { useAuthorizedMutationFetch } from "../../../contexts/AuthorizedFetchContext";

export interface UseDeleteOrganizationSenderSettingsMutationProps {
    orgName: string;
    settings: RSSender;
}

export const useDeleteOrganizationSenderSettings = () => {
    const { authorizedFetch, rsUseMutation } = useAuthorizedMutationFetch<
        RSSender,
        unknown,
        UseDeleteOrganizationSenderSettingsMutationProps
    >();
    const mutationFn = useCallback(
        async ({
            orgName,
            settings,
        }: UseDeleteOrganizationSenderSettingsMutationProps) => {
            const res = await authorizedFetch(settingsEndpoints.deleteSender, {
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

    return rsUseMutation([settingsEndpoints.deleteSender.queryKey], mutationFn);
};
