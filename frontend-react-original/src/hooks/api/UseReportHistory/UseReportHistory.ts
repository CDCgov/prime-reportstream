import { useSuspenseQuery } from "@tanstack/react-query";
import { RSOrganizationSettings } from "../../../config/endpoints/settings";
import useSessionContext from "../../../contexts/Session/useSessionContext";

export interface RSReportHistorySearchParams {}

export interface RSReportAction {
    date: string;
    user: string;
    action: string | undefined;
}

export interface RSReportHistory {
    sent: number;
    via: string;
    // positive: number1;
    total: number;
    fileType: string;
    type: string;
    reportId: string;
    expires: number;
    sendingOrg: string;
    receivingOrg: string;
    receivingOrgSvc: string;
    facilities: string[];
    actions: RSReportAction[];
    displayName: string;
    content: string;
    fileName: string;
    mimeType: string;
}

// TODO Implement in pages
const useReportHistory = (params: RSReportHistorySearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSOrganizationSettings[]>({
            url: `/history/report`,
            params,
        });
    };

    return useSuspenseQuery({
        queryKey: ["reportHistory", params],
        queryFn: fn,
    });
};

export default useReportHistory;
