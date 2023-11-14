import * as UseOrganizationReceiversHook from "../../../UseOrganizationReceivers";
import * as UseOrganizationReceiversFeed from "../../../UseOrganizationReceiversFeed";

export const mockUseOrganizationReceivers = vi.spyOn(
    UseOrganizationReceiversHook,
    "useOrganizationReceivers",
);
vi.mock("../../../UseOrganizationReceivers", async (imp) => ({
    ...(await imp()),
    UseOrganizationReceivers: vi.fn(),
}));
vi.mock("../../../UseOrganizationReceiversFeed", async (imp) => ({
    ...(await imp()),
    useOrganizationReceiversFeed: vi.fn(),
}));

export const mockUseOrganizationReceiversFeed = vi.mocked(
    UseOrganizationReceiversFeed.useOrganizationReceiversFeed,
);
export const mockUseOrganizationReceiversHook = vi.mocked(
    UseOrganizationReceiversHook.useOrganizationReceivers,
);
