import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import { RSReceiver, servicesEndpoints } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { CustomerStatusType } from "../../../../utils/DataDashboardUtils";
import { Organizations } from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { receivers } = servicesEndpoints;

const useOrganizationReceivers = () => {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const parsedName = activeMembership?.parsedName;
    const isAdmin = Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;

    const memoizedDataFetch = useCallback(() => {
        if (parsedName && !isAdmin) {
            return authorizedFetch<RSReceiver[]>(
                {
                    segments: {
                        orgName: parsedName,
                    },
                },
                receivers,
            );
        }
        return null;
    }, [isAdmin, authorizedFetch, parsedName]);
    const useSuspenseQueryResult = useSuspenseQuery({
        queryKey: [receivers.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });

    const { data } = useSuspenseQueryResult;
    const allReceivers = (data ?? []).sort((a, b) => a.name.localeCompare(b.name));
    const activeReceivers = allReceivers.filter((receiver) => receiver.customerStatus !== CustomerStatusType.INACTIVE);

    return {
        ...useSuspenseQueryResult,
        allReceivers: allReceivers,
        activeReceivers: activeReceivers,
        isDisabled: isAdmin,
    };
};

export default useOrganizationReceivers;
