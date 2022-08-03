import ReportsApi, {
    ReportDetailParams,
    RSReportInterface,
} from "../../../network/api/History/Reports";
import { useApiEndpoint } from "../UseApiEndpoint";

const useReportsList = () =>
    useApiEndpoint<{}, RSReportInterface[]>(ReportsApi, "list", "GET");
const useReportsDetail = (reportId: string) =>
    useApiEndpoint<ReportDetailParams, RSReportInterface>(
        ReportsApi,
        "detail",
        "GET",
        {
            id: reportId,
        }
    );

export { useReportsList, useReportsDetail };
