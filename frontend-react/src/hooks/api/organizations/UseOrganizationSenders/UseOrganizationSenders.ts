import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import {
    RSSender,
    servicesEndpoints,
} from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { senders } = servicesEndpoints;

export type UseOrganizationSendersResult = ReturnType<
    typeof useOrganizationSenders
>;

export default function useOrganizationSenders() {
    const { activeMembership, authorizedFetch } = useSessionContext();

    const memoizedDataFetch = useCallback(() => {
        if (activeMembership?.parsedName) {
            return authorizedFetch<RSSender[]>(
                {
                    segments: {
                        orgName: activeMembership.parsedName,
                    },
                },
                senders,
            );
        }
        return null;
    }, [activeMembership?.parsedName, authorizedFetch]);
    return useSuspenseQuery({
        queryKey: [senders.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });
}
