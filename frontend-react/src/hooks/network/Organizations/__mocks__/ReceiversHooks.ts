import * as UseOrganizationReceiversHook from "../../../UseOrganizationReceivers";
import * as UseOrganizationReceiversFeedHook from "../../../UseOrganizationReceiversFeed";

export const mockUseOrganizationReceivers = vi.spyOn(
    UseOrganizationReceiversHook,
    "useOrganizationReceivers",
);

export const mockUseOrganizationReceiversFeed = vi.spyOn(
    UseOrganizationReceiversFeedHook,
    "useOrganizationReceiversFeed",
);
