import AdminReceiverDashboardPage from "./AdminReceiverDashboardPage";
import useReceiversConnectionStatus from "../../../../hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus";
import { renderApp, screen } from "../../../../utils/CustomRenderUtils";
import { mockReceiversStatuses } from "../fixtures";

vi.mock("../../../../hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus.ts", () => ({
    default: vi.fn(),
}));

const _ = vi.mocked(useReceiversConnectionStatus).mockImplementation(() => ({ data: mockReceiversStatuses }) as any);

describe("AdminReceiverDashboard", () => {
    test("renders", () => {
        renderApp(<AdminReceiverDashboardPage />);

        expect(screen.getByText("Receiver Status Dashboard")).toBeVisible();
    });
});
