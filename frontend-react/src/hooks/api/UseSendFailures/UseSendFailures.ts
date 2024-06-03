import { useSuspenseQuery } from "@tanstack/react-query";
import { RSOrganizationSettings } from "../../../config/endpoints/settings";
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
    daysToShow: number;
}

// TODO Implement in pages
const useSendFailures = (params: RSSendFailuresSearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSOrganizationSettings[]>({
            url: `/adm/getsendfailures`,
            params,
        });
    };

    return useSuspenseQuery({
        queryKey: ["sendFailures", params],
        queryFn: fn,
    });
};

export default useSendFailures;
