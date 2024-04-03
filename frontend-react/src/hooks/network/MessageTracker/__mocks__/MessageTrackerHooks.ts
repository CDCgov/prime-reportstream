import * as MessageTrackerHooks from "../MessageTrackerHooks";

export const mockUseMessageDetails = vi.spyOn(
    MessageTrackerHooks,
    "useMessageDetails",
);
