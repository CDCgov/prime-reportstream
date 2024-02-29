import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import { Organizations } from "./UseAdminSafeOrganizationName";
import { RSReceiver, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";
import { CustomerStatusType } from "../utils/DataDashboardUtils";

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
    const useSuspenseQueryResult = useSuspenseQuery({
        queryKey: [receivers.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });

    const { data } = useSuspenseQueryResult;
    const allReceivers = (data ?? []).sort((a, b) =>
        a.name.localeCompare(b.name),
    );
    const activeReceivers = allReceivers.filter(
        (receiver) => receiver.customerStatus !== CustomerStatusType.INACTIVE,
    );

    return {
        ...useSuspenseQueryResult,
        allReceivers: allReceivers,
        activeReceivers: activeReceivers,
        isDisabled: isAdmin,
    };
};
