import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import { RSSender, servicesEndpoints } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { senderDetail } = servicesEndpoints;

export type UseSenderResourceHookResult = ReturnType<typeof useOrganizationSender>;

export default function useOrganizationSender(initialData?: RSSender) {
    /* Access the session. */
    const { activeMembership, authorizedFetch } = useSessionContext();
    const memoizedDataFetch = useCallback(() => {
        if (!!activeMembership?.parsedName && !!activeMembership.service) {
            return authorizedFetch<RSSender>(
                {
                    segments: {
                        orgName: activeMembership.parsedName,
                        sender: activeMembership.service,
                    },
                },
                senderDetail,
            );
        }
        return null;
    }, [activeMembership?.parsedName, activeMembership?.service, authorizedFetch]);
    return useSuspenseQuery({
        queryKey: [senderDetail.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
        initialData: initialData,
    });
}
