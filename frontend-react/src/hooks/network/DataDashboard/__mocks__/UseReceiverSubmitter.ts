import * as UseReceiverSubmitter from "../UseReceiverSubmitters";

vi.mock("../UseReceiverSubmitters", async (imp) => ({
    ...(await imp()),
    default: vi.fn(),
}));

export const mockUseReceiverSubmitter = vi.mocked(UseReceiverSubmitter.default);
