import { useSuspenseQuery } from "@tanstack/react-query";
import { RSOrganizationSettings } from "../../../config/endpoints/settings";
import useSessionContext from "../../../contexts/Session/useSessionContext";

export interface RSActionHistory {
    submissionId: number;
    timestamp: string; //comes in format yyyy-mm-ddThh:mm:ss.sssZ
    sender: string | undefined;
    httpStatus: number;
    externalName: string;
    id: string;
    destinations: Destination[];
    errors: ReportError[];
    warnings: ReportWarning[];
    topic: string;
    warningCount: number;
    errorCount: number;
}

export interface Destination {
    organization_id: string;
    organization: string;
    service: string;
    filteredReportRows: string[];
    filteredReportItems: FilteredReportItem[];
    sending_at?: string;
    itemCount: number;
    itemCountBeforeQualityFiltering: number;
    sentReports: string[];
}

export interface FilteredReportItem {
    filterType: string;
    filterName: string;
    filteredTrackingElement: string;
    filterArgs: string[];
    message: string;
}

export interface ReportWarning {
    scope: string;
    errorCode: string;
    type: string;
    message: string;
}

export interface ReportError extends ReportWarning {
    index: number;
    trackingId: string;
}

export interface RSActionHistorySearchParams {
    actionId: string;
    organization: string;
}

// TODO Implement in pages
const useActionHistory = (params?: RSActionHistorySearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSOrganizationSettings[]>({
            url: `/waters/report/${params?.actionId}/history`,
            params,
        });
    };

    return useSuspenseQuery({
        queryKey: ["actionHistory", params],
        queryFn: fn,
    });
};

export default useActionHistory;
