import * as DeliveryHooks from "../DeliveryHooks";

export const mockUseOrgDeliveries = jest.spyOn(
    DeliveryHooks,
    "useOrgDeliveries"
);

export const mockUseReportDetail = jest.spyOn(
    DeliveryHooks,
    "useReportsDetail"
);

export const mockUseReportFacilities = jest.spyOn(
    DeliveryHooks,
    "useReportsFacilities"
);
