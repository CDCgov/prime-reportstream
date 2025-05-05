import { useSuspenseQuery } from "@tanstack/react-query";
import { endOfDay, startOfDay } from "date-fns";
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
    startDate: Date;
    endDate?: Date;
}

const useReceiversConnectionStatus = ({ startDate, endDate }: RSReceiverStatusSearchParams) => {
    const { authorizedFetch } = useSessionContext();
    const fixedParams = {
        start_date: startOfDay(startDate),
        end_date: endDate ? endOfDay(endDate) : endDate,
    };

    const fn = () => {
        return authorizedFetch<RSReceiverStatus[]>({
            url: `/adm/listreceiversconnstatus`,
            params: fixedParams,
        });
    };

    return useSuspenseQuery({
        queryKey: ["receiversConnectionStatus", fixedParams],
        queryFn: fn,
    });
};

export default useReceiversConnectionStatus;
