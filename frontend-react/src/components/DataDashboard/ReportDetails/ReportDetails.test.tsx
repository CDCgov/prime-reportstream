import { ReportDetailsPage } from "./ReportDetails";
import { RSDelivery } from "../../../config/endpoints/deliveries";

import useReportsDetail from "../../../hooks/api/deliveries/UseReportDetail/UseReportDetail";
import useReportsFacilities from "../../../hooks/api/deliveries/UseReportFacilities/UseReportFacilities";
import { renderApp } from "../../../utils/CustomRenderUtils";

const TEST_ID = "123";

vi.mock("react-router-dom", async (importActual) => ({
    ...(await importActual<typeof import("react-router-dom")>()), // use actual for all non-hook parts
    useParams: () => ({
        reportId: TEST_ID,
    }),
}));
vi.mock("../../../hooks/api/deliveries/UseReportDetail/UseReportDetail");
vi.mock(
    "../../../hooks/api/deliveries/UseReportFacilities/UseReportFacilities",
);

const mockUseReportDetail = vi.mocked(useReportsDetail);
const mockUseReportFacilities = vi.mocked(useReportsFacilities);

describe("ReportDetails", () => {
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportDetail.mockReturnValue({
            data: {} as RSDelivery,
        } as any);
        mockUseReportFacilities.mockReturnValue({
            data: [],
        } as any);

        renderApp(<ReportDetailsPage />);
        expect(mockUseReportDetail).toHaveBeenCalledWith(TEST_ID);
    });
});
