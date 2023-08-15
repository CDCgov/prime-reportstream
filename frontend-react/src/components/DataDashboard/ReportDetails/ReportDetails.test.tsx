import { mockUseReportDetail } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { RSDelivery } from "../../../config/endpoints/deliveries";

import { ReportDetails } from "./ReportDetails";

const TEST_ID = "123";

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"), // use actual for all non-hook parts
    useParams: () => ({
        reportId: TEST_ID,
    }),
}));

describe("ReportDetails", () => {
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportDetail.mockReturnValue({
            reportDetail: {} as RSDelivery,
        });

        renderApp(<ReportDetails />);
        expect(mockUseReportDetail).toHaveBeenCalledWith(TEST_ID);
    });
});
