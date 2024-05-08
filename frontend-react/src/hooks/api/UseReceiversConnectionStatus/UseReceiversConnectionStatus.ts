import { useSuspenseQuery } from "@tanstack/react-query";
import useSessionContext from "../../../contexts/Session/useSessionContext";

export interface RSReceiverStatus {
    receiverConnectionCheckResultId: number;
    organizationId: number;
    receiverId: number;
    connectionCheckResult: string;
    connectionCheckSuccessful: boolean;
    connectionCheckStartedAt: string;
    connectionCheckCompletedAt: string;
    organizationName: string;
    receiverName: string;
}

export interface RSReceiverStatusSearchParams {
    startDate: string; // Date().toISOString
    endDate?: string | undefined; // Date().toISOString
}

const useReceiversConnectionStatus = (params: RSReceiverStatusSearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSReceiverStatus[]>({
            url: `/adm/listreceiversconnstatus`,
            params: {
                start_date: params.startDate,
                end_date: params.endDate,
            },
        });
    };

    return useSuspenseQuery({
        queryKey: ["receiversConnectionStatus", params],
        queryFn: fn,
    });
};

export default useReceiversConnectionStatus;
