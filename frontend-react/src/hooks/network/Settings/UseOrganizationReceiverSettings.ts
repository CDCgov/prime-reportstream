import { useCallback } from "react";

import {
    settingsEndpoints,
    RSSender,
} from "../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../../../contexts/SessionContext";

const { sender } = settingsEndpoints;

export const useOrganizationReceiverSettings = () => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSSender>();
    /* Access the session. */
    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(sender, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                    receiver: activeMembership?.service!!,
                },
            }),
        [
            activeMembership?.parsedName,
            activeMembership?.service,
            authorizedFetch,
        ]
    );
    return rsUseQuery([sender.queryKey, activeMembership], memoizedDataFetch, {
        enabled: !!activeMembership?.parsedName && !!activeMembership.service,
    });
};
