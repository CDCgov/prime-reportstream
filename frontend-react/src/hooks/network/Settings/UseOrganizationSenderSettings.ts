import { useCallback } from "react";

import {
    servicesEndpoints,
    RSSender,
} from "../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../../../contexts/SessionContext";

const { sender } = servicesEndpoints;

export type UseOrganizationSenderSettingsHookResult = {
    senderDetail?: RSSender;
    senderIsLoading: boolean;
    isInitialLoading: boolean;
};

export const useOrganizationSenderSettings =
    (): UseOrganizationSenderSettingsHookResult => {
        const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSSender>();
        /* Access the session. */
        const { activeMembership } = useSessionContext();
        const memoizedDataFetch = useCallback(
            () =>
                authorizedFetch(sender, {
                    segments: {
                        orgName: activeMembership?.parsedName!!,
                        sender: activeMembership?.service!!,
                    },
                }),
            [
                activeMembership?.parsedName,
                activeMembership?.service,
                authorizedFetch,
            ]
        );
        const { data, isLoading, isInitialLoading } = rsUseQuery(
            [sender.queryKey, activeMembership],
            memoizedDataFetch,
            {
                enabled:
                    !!activeMembership?.parsedName &&
                    !!activeMembership.service,
            }
        );

        return {
            senderDetail: data,
            senderIsLoading: isLoading,
            isInitialLoading,
        };
    };
