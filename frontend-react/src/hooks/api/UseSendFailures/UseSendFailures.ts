import { useSuspenseQuery } from "@tanstack/react-query";
import useSessionContext from "../../../contexts/Session/useSessionContext";

export interface RSSendFailure {
    actionId: number;
    /* the uuid for this report */
    reportId: string;
    /* Org destination name of the receiver that failed */
    receiver: string;
    /* Filename for the data that's prepared for forwarding but failing */
    fileName: string;
    /* the time that the particular error happened */
    failedAt: string;
    /* The original action that failed had a url. These are the cgi params. */
    actionParams: string;
    /* The long error message generated when the upload failed. */
    actionResult: string;
    /* The body portion of the original action url. Contains the location of the file that failed to forward */
    bodyUrl: string;
    /* The parsed receiver. It should be the same as receiver field above */
    reportFileReceiver: string;
}

export interface RSSendFailuresSearchParams {
    daysToShow?: number;
}

const useSendFailures = ({ daysToShow }: RSSendFailuresSearchParams = {}) => {
    const { authorizedFetch } = useSessionContext();
    const fixedParams = {
        days_to_show: daysToShow,
    };

    const fn = () => {
        return authorizedFetch<RSSendFailure[]>({
            url: `/adm/getsendfailures`,
            params: fixedParams,
        });
    };

    return useSuspenseQuery({
        queryKey: ["sendFailures", fixedParams],
        queryFn: fn,
    });
};

export default useSendFailures;
