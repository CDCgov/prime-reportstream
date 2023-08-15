import * as UseReceiverSubmitter from "../UseReceiverSubmitters";

export const mockUseReceiverSubmitter = jest.spyOn(
    UseReceiverSubmitter,
    "default",
);
