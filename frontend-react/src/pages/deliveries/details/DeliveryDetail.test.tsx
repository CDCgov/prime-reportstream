import {
    mockUseReportDetail,
    mockUseReportFacilities,
} from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { renderApp } from "../../../utils/CustomRenderUtils";
import * as UseOrganizationSettings from "../../../hooks/UseOrganizationSettings";
import { mockUseOktaAuth } from "../../../__mocks__/MockOkta";

import { DeliveryDetail } from "./DeliveryDetail";

const TEST_ID = "test-id-123";

const mockUseOrganizationSettings = jest.spyOn(
    UseOrganizationSettings,
    "useOrganizationSettings"
);

describe("DeliveryDetails", () => {
    /* Render tests for the Table component cover the generation of a table via config. The only untested
     * unit inside DeliveryFacilitiesTable was the link between the reportId and the hook we pass it into to
     * fetch data. */
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportDetail.mockReturnValue({
            reportDetail: {
                batchReadyAt: "",
                deliveryId: 0,
                expires: "",
                fileName: "",
                fileType: "",
                receiver: "",
                reportId: "",
                reportItemCount: 0,
                topic: "",
            },
        });
        mockUseReportFacilities.mockReturnValue({ reportFacilities: [] });
        mockUseOrganizationSettings.mockReturnValue({
            data: {
                createdAt: "",
                createdBy: "",
                description: "",
                filters: [],
                jurisdiction: "",
                name: "",
                version: 0,
                countyName: "",
                stateCode: "",
            },
        } as any);
        mockUseOktaAuth.mockReturnValue({ authState: {} } as any);
        renderApp(<DeliveryDetail />, {
            initialRouteEntries: [`/report-details/${TEST_ID}`],
        });
        expect(mockUseReportDetail).toHaveBeenCalledWith(TEST_ID);
    });
});
