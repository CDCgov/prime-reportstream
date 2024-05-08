import { useMutation } from "@tanstack/react-query";

import { RSOrganizationSettings } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

function useDeleteOrganizationSettings() {
    const { authorizedFetch } = useSessionContext();

    const fn = (
        obj: Partial<Omit<RSOrganizationSettings, "name">> & { name: string },
    ) => {
        return authorizedFetch({
            url: `/settings/organizations/${obj.name}`,
            method: "delete",
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useDeleteOrganizationSettings;
