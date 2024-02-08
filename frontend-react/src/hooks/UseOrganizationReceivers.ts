import { useCallback } from "react";
import { useQuery } from "@tanstack/react-query";

import { RSReceiver, servicesEndpoints } from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";
import { CustomerStatusType } from "../utils/DataDashboardUtils";

import { Organizations } from "./UseAdminSafeOrganizationName";

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
                    orgName: parsedName!!,
                },
            });
        }
        return null;
    }, [isAdmin, authorizedFetch, parsedName]);
    const { data, isLoading } = useQuery({
        queryKey: [receivers.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
        enabled: !isAdmin,
    });
    const allReceivers = (data || []).sort((a, b) =>
        a.name.localeCompare(b.name),
    );
    const activeReceivers = allReceivers.filter(
        (receiver) => receiver.customerStatus !== CustomerStatusType.INACTIVE,
    );
    return {
        allReceivers: allReceivers,
        activeReceivers: activeReceivers,
        isLoading: isLoading,
        isDisabled: isAdmin,
    };
};
