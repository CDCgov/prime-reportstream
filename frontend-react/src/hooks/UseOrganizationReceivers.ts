import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import { Organizations } from "./UseAdminSafeOrganizationName";
import { RSReceiver, servicesEndpoints } from "../config/endpoints/settings";
import useAuthorizedFetch from "../contexts/AuthorizedFetch/useAuthorizedFetch";
import useSessionContext from "../contexts/Session/useSessionContext";

const { receivers } = servicesEndpoints;

export const useOrganizationReceivers = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;
    const isAdmin =
        Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;

    const authorizedFetch = useAuthorizedFetch<RSReceiver[]>();
    const memoizedDataFetch = useCallback(() => {
        if (parsedName && !isAdmin) {
            return authorizedFetch(receivers, {
                segments: {
                    orgName: parsedName,
                },
            });
        }
        return null;
    }, [isAdmin, authorizedFetch, parsedName]);
    return {
        ...useSuspenseQuery({
            queryKey: [receivers.queryKey, activeMembership],
            queryFn: memoizedDataFetch,
        }),
        isDisabled: isAdmin,
    };
};
