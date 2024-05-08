import { useMutation } from "@tanstack/react-query";

import { RSOrganizationSettings } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

function useUpdateOrganizationSettings() {
    const { authorizedFetch } = useSessionContext();

    const fn = (
        data: Partial<Omit<RSOrganizationSettings, "name">> & { name: string },
    ) => {
        return authorizedFetch({
            url: `/settings/organizations/${data.name}`,
            method: "put",
            data,
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useUpdateOrganizationSettings;
