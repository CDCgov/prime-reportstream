import { ReportDetailsPage } from "./ReportDetails";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import {
    mockUseReportDetail,
    mockUseReportFacilities,
} from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { renderApp } from "../../../utils/CustomRenderUtils";

const TEST_ID = "123";

vi.mock("react-router-dom", async (importActual) => ({
    ...(await importActual<typeof import("react-router-dom")>()), // use actual for all non-hook parts
    useParams: () => ({
        reportId: TEST_ID,
    }),
}));

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
