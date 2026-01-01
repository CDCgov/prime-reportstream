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
    daysToShow?: number;
}

const useResends = ({ daysToShow }: RSResendsSearchParams = {}) => {
    const { authorizedFetch } = useSessionContext();
    const fixedParams = {
        days_to_show: daysToShow,
    };

    const fn = () => {
        return authorizedFetch<RSResend[]>({
            url: `/adm/getresend`,
            params: fixedParams,
        });
    };

    return useSuspenseQuery({
        queryKey: ["resends", fixedParams],
        queryFn: fn,
    });
};

export default useResends;
