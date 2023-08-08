import * as UseOrganizationReceiversHook from "../../../UseOrganizationReceivers";
import * as UseOrganizationReceiversFeedHook from "../../../UseOrganizationReceiversFeed";

export const mockUseOrganizationReceivers = jest.spyOn(
    UseOrganizationReceiversHook,
    "useOrganizationReceivers",
);

export const mockUseOrganizationReceiversFeed = jest.spyOn(
    UseOrganizationReceiversFeedHook,
    "useOrganizationReceiversFeed",
);
