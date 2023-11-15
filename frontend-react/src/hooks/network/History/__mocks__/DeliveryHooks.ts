import * as DeliveryHooks from "../DeliveryHooks";

vi.mock("../DeliveryHooks", async (imp) => ({
    ...(await imp<typeof import("../DeliveryHooks")>()),
    useOrgDeliveries: vi.fn(),
    useReportsDetail: vi.fn(),
    useReportsFacilities: vi.fn(),
}));

export const mockUseOrgDeliveries = vi.mocked(DeliveryHooks.useOrgDeliveries);

export const mockUseReportsDetail = vi.mocked(DeliveryHooks.useReportsDetail);

export const mockUseReportsFacilities = vi.mocked(
    DeliveryHooks.useReportsFacilities,
);
