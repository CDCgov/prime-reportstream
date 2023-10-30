import * as UseReceiverSubmitter from "../UseReceiverSubmitters";

export const mockUseReceiverSubmitter = vi.spyOn(
    UseReceiverSubmitter,
    "default",
);
