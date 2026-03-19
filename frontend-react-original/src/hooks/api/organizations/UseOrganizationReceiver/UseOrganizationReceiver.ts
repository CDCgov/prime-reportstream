import { useSuspenseQuery } from "@tanstack/react-query";

import { RSReceiver } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

// TODO Implement in pages
function useOrganizationReceiver(receiverId: string, orgId?: string) {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const org = orgId ?? activeMembership?.parsedName;

    if (!org) throw new Error("Invalid request");

    const fn = () => {
        return authorizedFetch<RSReceiver[]>({
            url: `/settings/organizations/${org}/${receiverId}`,
        });
    };
    const result = useSuspenseQuery({
        queryKey: ["organizationReceiver", orgId, receiverId],
        queryFn: fn,
    });

    return result;
}

export default useOrganizationReceiver;
