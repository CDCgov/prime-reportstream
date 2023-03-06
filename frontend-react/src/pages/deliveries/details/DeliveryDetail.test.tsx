import { mockUseReportDetail } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import { renderApp } from "../../../utils/CustomRenderUtils";

import { DeliveryDetail } from "./DeliveryDetail";

const TEST_ID = "test-id-123";
jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"), // use actual for all non-hook parts
    useParams: () => ({
        reportId: TEST_ID,
    }),
}));

describe("DeliveryDetails", () => {
    /* Render tests for the Table component cover the generation of a table via config. The only untested
     * unit inside DeliveryFacilitiesTable was the link between the reportId and the hook we pass it into to
     * fetch data. */
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportDetail.mockReturnValue({
            reportDetail: {} as RSDelivery,
        });
        renderApp(<DeliveryDetail />);
        expect(mockUseReportDetail).toHaveBeenCalledWith(TEST_ID);
    });
});
