import { useCallback } from "react";
import { useQuery } from "@tanstack/react-query";

import { RSReceiver, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";

import { Organizations } from "./UseAdminSafeOrganizationName";

const { receivers } = servicesEndpoints;

export const useOrganizationReceivers = (orgName?: string) => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;
    const isAdmin =
        Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;
    const isEnabled = !!orgName || (!isAdmin && !!parsedName);

    const authorizedFetch = useAuthorizedFetch<RSReceiver[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(receivers, {
                segments: {
                    orgName: orgName ?? parsedName!!,
                },
            }),
        [authorizedFetch, orgName, parsedName],
    );

    return {
        ...useQuery({
            queryKey: [receivers.queryKey, orgName ?? activeMembership],
            queryFn: memoizedDataFetch,
            enabled: isEnabled,
        }),
        isDisabled: !isEnabled,
    };
};
