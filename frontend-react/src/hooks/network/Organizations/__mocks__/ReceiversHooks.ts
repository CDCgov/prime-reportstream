import * as ReceiverHooks from "../ReceiversHooks";
import * as UseOrganizationReceiversHook from "../../../UseOrganizationReceivers";
import * as UseOrganizationReceiversFeedHook from "../../../UseOrganizationReceiversFeed";

export const mockReceiverHook = jest.spyOn(ReceiverHooks, "useReceiversList");

export const mockUseOrganizationReceivers = jest.spyOn(
    UseOrganizationReceiversHook,
    "useOrganizationReceivers"
);

export const mockUseOrganizationReceiversFeed = jest.spyOn(
    UseOrganizationReceiversFeedHook,
    "useOrganizationReceiversFeed"
);
