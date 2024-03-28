import * as DeliveryHooks from "../DeliveryHooks";

export const mockUseOrgDeliveries = vi.spyOn(DeliveryHooks, "useOrgDeliveries");

export const mockUseReportDetail = vi.spyOn(DeliveryHooks, "useReportsDetail");

export const mockUseReportFacilities = vi.spyOn(
    DeliveryHooks,
    "useReportsFacilities",
);
