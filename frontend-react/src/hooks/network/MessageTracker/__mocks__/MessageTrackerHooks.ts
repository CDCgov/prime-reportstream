import * as MessageTrackerHooks from "../MessageTrackerHooks";

vi.mock("../MessageTrackerHooks", async (imp) => ({
    ...(await imp<typeof import("../MessageTrackerHooks")>()),
    useMessageDetails: vi.fn(),
}));

export const mockUseMessageDetails = vi.mocked(
    MessageTrackerHooks.useMessageDetails,
);
