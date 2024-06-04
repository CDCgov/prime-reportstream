import { useMutation } from "@tanstack/react-query";

import { HTTPMethods } from "../../../../config/endpoints";
import { RSReceiver } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

// TODO Implement in pages
function useDeleteOrganizationReceiver() {
    const { authorizedFetch } = useSessionContext();

    const fn = (
        obj: Partial<Omit<RSReceiver, "name" | "organizationName">> & {
            name: string;
            organizationName: string;
        },
    ) => {
        return authorizedFetch({
            url: `/settings/organizations/${obj.organizationName}/receivers/${obj.name}`,
            method: HTTPMethods.DELETE,
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useDeleteOrganizationReceiver;
