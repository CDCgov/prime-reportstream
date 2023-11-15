import * as UseReceiverDeliveries from "../UseReceiverDeliveries";

vi.mock("../UseReceiverDeliveries", async (imp) => ({
    ...(await imp<typeof import("../UseReceiverDeliveries")>()),
    default: vi.fn(),
}));

export const mockUseReceiverDeliveries = vi.mocked(
    UseReceiverDeliveries.default,
);
