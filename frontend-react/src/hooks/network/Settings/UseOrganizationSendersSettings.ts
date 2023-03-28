import { useCallback } from "react";

import {
    settingsEndpoints,
    RSSender,
} from "../../../config/endpoints/settings";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../../../contexts/SessionContext";
import { Organizations } from "../../UseAdminSafeOrganizationName";

const { senders } = settingsEndpoints;

export const useOrganizationSendersSettings = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSSender[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senders, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch]
    );
    return rsUseQuery([senders.queryKey, activeMembership], memoizedDataFetch, {
        enabled:
            Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
    });
};
