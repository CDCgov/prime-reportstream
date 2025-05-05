import { useMutation } from "@tanstack/react-query";

import { HTTPMethods } from "../../../../config/endpoints";
import { RSOrganizationSettings } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

function useUpdateOrganizationSettings() {
    const { authorizedFetch } = useSessionContext();

    const fn = (data: Partial<Omit<RSOrganizationSettings, "name">> & { name: string }) => {
        return authorizedFetch({
            url: `/settings/organizations/${data.name}`,
            method: HTTPMethods.PUT,
            data,
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useUpdateOrganizationSettings;
