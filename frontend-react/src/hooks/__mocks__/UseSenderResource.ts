import * as SenderHooks from "../api/Settings/UseOrganizationSenderSettings";

export const mockUseSenderResource = jest.spyOn(
    SenderHooks,
    "useOrganizationSenderSettings"
);
