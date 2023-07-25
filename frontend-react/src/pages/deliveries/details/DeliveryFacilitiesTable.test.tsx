import { mockUseReportFacilities } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { renderApp } from "../../../utils/CustomRenderUtils";

import DeliveryFacilitiesTable from "./DeliveryFacilitiesTable";

const TEST_ID = "test-id-123";

describe("DeliveryFacilitiesTable", () => {
    /* Render tests for the Table component cover the generation of a table via config. The only untested
     * unit inside DeliveryFacilitiesTable was the link between the reportId and the hook we pass it into to
     * fetch data. */
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportFacilities.mockReturnValue({
            reportFacilities: [],
        });
        renderApp(<DeliveryFacilitiesTable reportId={TEST_ID} />);
        expect(mockUseReportFacilities).toHaveBeenCalledWith(TEST_ID);
    });
});
