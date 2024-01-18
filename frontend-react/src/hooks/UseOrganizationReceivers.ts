import { useCallback } from "react";
import { useQuery } from "@tanstack/react-query";

import { RSReceiver, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";

import { Organizations } from "./UseAdminSafeOrganizationName";

const { receivers } = servicesEndpoints;

export const useOrganizationReceivers = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const authorizedFetch = useAuthorizedFetch<RSReceiver[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(receivers, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch],
    );
    const isAdmin =
        Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;
    return {
        ...useQuery({
            queryKey: [receivers.queryKey, activeMembership],
            queryFn: memoizedDataFetch,
            enabled: !isAdmin,
        }),
        isDisabled: isAdmin,
    };
};
