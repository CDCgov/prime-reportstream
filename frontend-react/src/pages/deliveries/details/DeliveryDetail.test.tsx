import { useParams } from "react-router-dom";

import { mockUseReportsDetail } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import { render } from "../../../utils/Test/render";

import DeliveryDetailPage from "./DeliveryDetail";

const TEST_ID = "test-id-123";

vi.mock("./DeliveryFacilitiesTable");

describe("DeliveryDetails", () => {
    beforeEach(() => {
        vi.mocked(useParams).mockReturnValue({ reportId: TEST_ID });
    });
    /* Render tests for the Table component cover the generation of a table via config. The only untested
     * unit inside DeliveryFacilitiesTable was the link between the reportId and the hook we pass it into to
     * fetch data. */
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportsDetail.mockReturnValue({
            data: {} as RSDelivery,
        } as any);
        render(<DeliveryDetailPage />);
        expect(mockUseReportsDetail).toHaveBeenCalledWith(TEST_ID);
    });
});
