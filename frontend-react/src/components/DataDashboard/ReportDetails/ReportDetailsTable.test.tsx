import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../../utils/CustomRenderUtils";
import { mockUseReportFacilities } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { makeFacilityFixtureArray } from "../../../__mocks__/DeliveriesMockServer";
import { mockAppInsights } from "../../../utils/__mocks__/ApplicationInsights";

import ReportDetailsTable from "./ReportDetailsTable";

const TEST_ID = "123";

jest.mock("../../../TelemetryService", () => ({
    ...jest.requireActual("../../../TelemetryService"),
    getAppInsights: () => mockAppInsights,
}));

describe("ReportDetailsTable", () => {
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportFacilities.mockReturnValue({
            reportFacilities: [],
        });
        renderApp(<ReportDetailsTable reportId={TEST_ID} />);
        expect(mockUseReportFacilities).toHaveBeenCalledWith(TEST_ID);
    });

    describe("with data", () => {
        beforeEach(() => {
            const mockUseReportFacilitiesCallback = {
                reportFacilities: makeFacilityFixtureArray(10),
            };

            mockUseReportFacilities.mockReturnValue(
                mockUseReportFacilitiesCallback,
            );

            // Render the component
            renderApp(<ReportDetailsTable reportId={TEST_ID} />);
        });

        test("renders 10 results per page + 1 header row", () => {
            // renders 10 results per page + 1 header row regardless of the total number of records
            // assuming our pagination limit is set to 10
            const rows = screen.getAllByRole("row");
            expect(rows).toHaveLength(10 + 1);
        });

        test("renders table with pagination", () => {
            expect(screen.getByText("Facility")).toBeInTheDocument();
            expect(screen.getByText("Location")).toBeInTheDocument();
            expect(screen.getByText("CLIA")).toBeInTheDocument();
            expect(screen.getByText("Total tests")).toBeInTheDocument();
            expect(screen.getByText("Total positive")).toBeInTheDocument();
        });

        describe("TableFilter", () => {
            test("Clicking on filter invokes the trackAppInsightEvent", async () => {
                await userEvent.click(screen.getByText("Filter"));

                expect(mockAppInsights.trackEvent).toBeCalledWith({
                    name: "Report Details | Table Filter",
                    properties: {
                        tableFilter: {
                            endRange: "3000-01-01T23:59:59.999Z",
                            startRange: "2000-01-01T00:00:00.000Z",
                        },
                    },
                });
            });
        });
    });

    describe("without data", () => {
        beforeEach(() => {
            const mockUseReportFacilitiesCallback = {
                reportFacilities: [],
            };

            mockUseReportFacilities.mockReturnValue(
                mockUseReportFacilitiesCallback,
            );

            // Render the component
            renderApp(<ReportDetailsTable reportId={TEST_ID} />);
        });

        test("renders table header row", () => {
            expect(screen.getByRole("row")).toBeInTheDocument();
            expect(screen.getByText("No data to show")).toBeInTheDocument();
        });
    });
});
