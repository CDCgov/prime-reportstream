import DeliveryFacilitiesTable from "./DeliveryFacilitiesTable";
import useReportsFacilities from "../../../hooks/api/deliveries/UseReportFacilities/UseReportFacilities";
import { renderApp } from "../../../utils/CustomRenderUtils";

const TEST_ID = "test-id-123";

vi.mock("../../../hooks/api/deliveries/UseReportFacilities/UseReportFacilities");
const mockUseReportFacilities = vi.mocked(useReportsFacilities);

describe("DeliveryFacilitiesTable", () => {
    /* Render tests for the Table component cover the generation of a table via config. The only untested
     * unit inside DeliveryFacilitiesTable was the link between the reportId and the hook we pass it into to
     * fetch data. */
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportFacilities.mockReturnValue({
            data: [],
        } as any);
        renderApp(<DeliveryFacilitiesTable reportId={TEST_ID} />);
        expect(mockUseReportFacilities).toHaveBeenCalledWith(TEST_ID);
    });
});
