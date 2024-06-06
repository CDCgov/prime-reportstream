import { DeliveryDetailPage } from "./DeliveryDetail";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import useReportsDetail from "../../../hooks/api/deliveries/UseReportDetail/UseReportDetail";
import { renderApp } from "../../../utils/CustomRenderUtils";

const TEST_ID = "test-id-123";
vi.mock("react-router-dom", async (importActual) => ({
    ...(await importActual<typeof import("react-router-dom")>()), // use actual for all non-hook parts
    useParams: () => ({
        reportId: TEST_ID,
    }),
}));

vi.mock("../../../hooks/api/deliveries/UseReportDetail/UseReportDetail");

const mockUseReportDetail = vi.mocked(useReportsDetail);

describe("DeliveryDetails", () => {
    /* Render tests for the Table component cover the generation of a table via config. The only untested
     * unit inside DeliveryFacilitiesTable was the link between the reportId and the hook we pass it into to
     * fetch data. */
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportDetail.mockReturnValue({
            data: {} as RSDelivery,
        } as any);
        renderApp(<DeliveryDetailPage />);
        expect(mockUseReportDetail).toHaveBeenCalledWith(TEST_ID);
    });
});
