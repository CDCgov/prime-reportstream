import { historyEndpoints } from "../../../config/api/history";
import { useRSQuery } from "../UseRSQuery";

export const useHistorySummaryTests = () => {
    return useRSQuery(historyEndpoints.summaryTests);
};
