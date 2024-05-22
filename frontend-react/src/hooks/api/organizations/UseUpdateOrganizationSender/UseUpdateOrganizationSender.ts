import { useMutation } from "@tanstack/react-query";

import { HTTPMethods } from "../../../../config/endpoints";
import { RSSender } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

function useUpdateOrganizationSender() {
    const { authorizedFetch } = useSessionContext();

    const fn = (
        data: Partial<Omit<RSSender, "name" | "organizationName">> & {
            name: string;
            organizationName: string;
        },
    ) => {
        return authorizedFetch({
            url: `/settings/organizations/${data.organizationName}/senders/${data.name}`,
            method: HTTPMethods.PUT,
            data,
        });
    };
    const result = useMutation({
        mutationFn: fn,
    });

    return result;
}

export default useUpdateOrganizationSender;
