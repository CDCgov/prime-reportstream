import * as UseOrgDeliveries from "../UseOrganizationDeliveries";
import * as UseReportsDetail from "../UseReportsDetail";
import * as UseReportsFacilities from "../UseReportsFacilities";
import * as UseReportHistory from "../UseReportHistory";

export const mockUseOrgDeliveries = jest.spyOn(
    UseOrgDeliveries,
    "useOrgDeliveries"
);

export const mockUseReportDetail = jest.spyOn(
    UseReportsDetail,
    "useReportsDetail"
);

export const mockUseReportFacilities = jest.spyOn(
    UseReportsFacilities,
    "useReportsFacilities"
);

export const mockUseReportHistory = jest.spyOn(
    UseReportHistory,
    "useReportHistory"
);
