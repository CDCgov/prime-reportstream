import { historyEndpoints } from "../../../config/api/history";
import { useRSQuery } from "../UseRSQuery";

export const useHistoryReportFacilities = (reportId: string) => {
    return useRSQuery(historyEndpoints.reportFacilities, {
        segments: { reportId },
    });
};
