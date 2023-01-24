import { historyEndpoints } from "../../../config/api/history";
import { useRSQuery } from "../UseRSQuery";

export const useHistoryReport = (reportId: string) => {
    return useRSQuery(historyEndpoints.report, { segments: { reportId } });
};
