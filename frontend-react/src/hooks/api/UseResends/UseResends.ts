import { useSuspenseQuery } from "@tanstack/react-query";
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
    daysToShow: number | string;
}

const useResends = (params?: RSResendsSearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSResend[]>({
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
