import { useMutation } from "@tanstack/react-query";

import { HTTPMethods } from "../../../../config/endpoints";
import { RSReceiver } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

function useUpdateOrganizationReceiver() {
    const { authorizedFetch } = useSessionContext();

    const fn = (
        data: Partial<Omit<RSReceiver, "name" | "organizationName">> & {
            name: string;
            organizationName: string;
        },
    ) => {
        return authorizedFetch({
            url: `/settings/organizations/${data.organizationName}/receivers/${data.name}`,
            method: HTTPMethods.PUT,
            data,
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useUpdateOrganizationReceiver;
