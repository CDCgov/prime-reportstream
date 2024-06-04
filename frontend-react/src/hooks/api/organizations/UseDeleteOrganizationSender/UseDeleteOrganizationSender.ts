import { useMutation } from "@tanstack/react-query";

import { HTTPMethods } from "../../../../config/endpoints";
import { RSSender } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

// TODO Implement in pages
function useDeleteOrganizationSender() {
    const { authorizedFetch } = useSessionContext();

    const fn = (
        obj: Partial<Omit<RSSender, "name" | "organizationName">> & {
            name: string;
            organizationName: string;
        },
    ) => {
        return authorizedFetch({
            url: `/settings/organizations/${obj.organizationName}/senders/${obj.name}`,
            method: HTTPMethods.DELETE,
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useDeleteOrganizationSender;
