import { render } from "@testing-library/react";

import { mockUseReportFacilities } from "../../hooks/network/History/__mocks__/DeliveryHooks";

import FacilitiesTable from "./FacilitiesTable";

const TEST_ID = "test-id-123";

describe("FacilitiesTable", () => {
    /* Render tests for the Table component cover the generation of a table via config. The only untested
     * unit inside FacilitiesTable was the link between the reportId and the hook we pass it into to
     * fetch data. */
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportFacilities.mockReturnValueOnce({
            reportFacilities: [],
        });
        render(<FacilitiesTable reportId={TEST_ID} />);
        expect(mockUseReportFacilities).toHaveBeenCalledWith(TEST_ID);
    });
});
