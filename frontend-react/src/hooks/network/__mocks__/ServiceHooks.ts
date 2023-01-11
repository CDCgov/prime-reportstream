import * as ServiceHooks from "../ServiceHooks";

export const mockUseMemberServices = jest.spyOn(
    ServiceHooks,
    "useMemberServices"
);
