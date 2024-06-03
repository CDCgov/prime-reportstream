import { useSuspenseQuery } from "@tanstack/react-query";
import { RSOrganizationSettings } from "../../../config/endpoints/settings";
import useSessionContext from "../../../contexts/Session/useSessionContext";

export interface RSResend {
    actionId: number; // BigInt
    actionName: string;
    createdAt: string;
    httpStatus: number;
    actionParams: string;
    actionResult: string;
    actionResponse: string;
    contentLength: number;
    senderIp: string;
    sendingOrg: string;
    sendingOrgClient: string;
    externalName: string;
    username: string;
}

export interface RSResendsSearchParams {
    daysToShow: number;
}

// TODO Implement in pages
const useResends = (params?: RSResendsSearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSOrganizationSettings[]>({
            url: `/adm/getresend`,
            params,
        });
    };

    return useSuspenseQuery({
        queryKey: ["resends", params],
        queryFn: fn,
    });
};

export default useResends;
