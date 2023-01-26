import * as UseOrgDeliveries from "../UseOrganizationDeliveries";
import * as UseReportsDetail from "../UseReportsDetail";
import * as UseReportsFacilities from "../UseReportFacilities";
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
    "useReportFacilities"
);

export const mockUseReportHistory = jest.spyOn(
    UseReportHistory,
    "useReportHistory"
);
