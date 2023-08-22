import { useCallback } from "react";

import { RSReceiver, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

import { Organizations } from "./UseAdminSafeOrganizationName";

const { receivers } = servicesEndpoints;

export const useOrganizationReceivers = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSReceiver[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(receivers, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch],
    );
    return rsUseQuery(
        [receivers.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
        },
    );
};
