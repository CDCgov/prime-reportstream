import * as MessageTrackerHooks from "../MessageTrackerHooks";

vi.mock("../MessageTrackerHooks", async (imp) => ({
    ...(await imp()),
    useMessageDetails: vi.fn(),
}));

export const mockUseMessageDetails = vi.mocked(
    MessageTrackerHooks.useMessageDetails,
);
