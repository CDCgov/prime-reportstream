import { useParams } from "react-router-dom";

import {
    mockUseReportsDetail,
    mockUseReportsFacilities,
} from "../../hooks/network/History/__mocks__/DeliveryHooks";
import { RSDelivery } from "../../config/endpoints/deliveries";
import { render } from "../../utils/Test/render";

import { ReportDetailsPage } from "./ReportDetails";

const TEST_ID = "123";

describe("ReportDetails", () => {
    test("url param (reportId) feeds into network hook", () => {
        vi.mocked(useParams).mockReturnValue({
            reportId: TEST_ID,
        });
        mockUseReportsDetail.mockReturnValue({
            data: {} as RSDelivery,
        } as any);
        mockUseReportsFacilities.mockReturnValue({
            data: [],
        } as any);

        render(<ReportDetailsPage />);
        expect(mockUseReportsDetail).toHaveBeenCalledWith(TEST_ID);
    });
});
