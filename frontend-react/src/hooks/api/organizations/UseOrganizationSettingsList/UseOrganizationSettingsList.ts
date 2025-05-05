import { useSuspenseQuery } from "@tanstack/react-query";
import { RSOrganizationSettings } from "../../../../config/endpoints/settings";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

export interface RSOrganizationSettingsSearchParams {
    organization: string;
}

const useOrganizationSettingsList = (params?: RSOrganizationSettingsSearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSOrganizationSettings[]>({
            url: `/settings/organizations`,
            params,
        });
    };

    return useSuspenseQuery({
        queryKey: ["organizationSettingsList", params],
        queryFn: fn,
    });
};

export default useOrganizationSettingsList;
