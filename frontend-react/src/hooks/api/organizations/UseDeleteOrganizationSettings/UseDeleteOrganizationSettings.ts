import { useMutation } from "@tanstack/react-query";

import { HTTPMethods } from "../../../../config/endpoints";
import { RSOrganizationSettings } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

// TODO Implement in pages
function useDeleteOrganizationSettings() {
    const { authorizedFetch } = useSessionContext();

    const fn = (obj: Partial<Omit<RSOrganizationSettings, "name">> & { name: string }) => {
        return authorizedFetch({
            url: `/settings/organizations/${obj.name}`,
            method: HTTPMethods.DELETE,
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useDeleteOrganizationSettings;
