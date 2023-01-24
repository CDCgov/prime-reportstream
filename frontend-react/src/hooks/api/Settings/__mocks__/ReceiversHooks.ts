import * as UseOrganizationReceiversHook from "../UseOrganizationReceiversSettings";
import * as UseOrganizationReceiversFeedHook from "../../../UseOrganizationReceiversFeed";

export const mockUseOrganizationReceivers = jest.spyOn(
    UseOrganizationReceiversHook,
    "useOrganizationReceiversSettings"
);

export const mockUseOrganizationReceiversFeed = jest.spyOn(
    UseOrganizationReceiversFeedHook,
    "useOrganizationReceiversFeed"
);
