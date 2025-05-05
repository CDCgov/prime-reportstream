import { screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import ReportDetailsTable from "./ReportDetailsTable";
import { makeFacilityFixtureArray } from "../../../__mockServers__/DeliveriesMockServer";
import useReportsFacilities from "../../../hooks/api/deliveries/UseReportFacilities/UseReportFacilities";
import useAppInsightsContext from "../../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { selectDatesFromRange } from "../../../utils/TestUtils";

vi.mock("../../../hooks/api/deliveries/UseReportFacilities/UseReportFacilities");

const mockUseAppInsightsContext = vi.mocked(useAppInsightsContext);
const mockAppInsights = mockUseAppInsightsContext();
const mockUseReportFacilities = vi.mocked(useReportsFacilities);

const TEST_ID = "123";

describe("ReportDetailsTable", () => {
    test("url param (reportId) feeds into network hook", () => {
        mockUseReportFacilities.mockReturnValue({
            data: [],
        } as any);
        renderApp(<ReportDetailsTable reportId={TEST_ID} />);
        expect(mockUseReportFacilities).toHaveBeenCalledWith(TEST_ID);
    });

    describe("with data", () => {
        function setup() {
            const mockUseReportFacilitiesCallback = {
                data: makeFacilityFixtureArray(10),
            };

            mockUseReportFacilities.mockReturnValue(mockUseReportFacilitiesCallback as any);

            // Render the component
            renderApp(<ReportDetailsTable reportId={TEST_ID} />);
        }

        test("renders 10 results per page + 1 header row", () => {
            setup();
            // renders 10 results per page + 1 header row regardless of the total number of records
            // assuming our pagination limit is set to 10
            const rows = screen.getAllByRole("row");
            expect(rows).toHaveLength(10 + 1);
        });

        test("renders table with pagination", () => {
            setup();
            expect(screen.getByText("Facility")).toBeInTheDocument();
            expect(screen.getByText("Location")).toBeInTheDocument();
            expect(screen.getByText("CLIA")).toBeInTheDocument();
            expect(screen.getByText("Total tests")).toBeInTheDocument();
            expect(screen.getByText("Total positive")).toBeInTheDocument();
        });

        describe("TableFilter", () => {
            test("Clicking on filter invokes the trackAppInsightEvent", async () => {
                setup();
                await selectDatesFromRange("20", "23");
                await userEvent.click(screen.getByText("Filter"));

                expect(mockAppInsights.trackEvent).toHaveBeenCalledWith({
                    name: "Report Details | Table Filter",
                    properties: {
                        tableFilter: {
                            endRange: "3000-01-23T23:59:59.999Z",
                            startRange: "2000-01-20T00:00:00.000Z",
                        },
                    },
                });
            });
        });
    });

    describe("without data", () => {
        function setup() {
            const mockUseReportFacilitiesCallback = {
                data: [],
            };

            mockUseReportFacilities.mockReturnValue(mockUseReportFacilitiesCallback as any);

            // Render the component
            renderApp(<ReportDetailsTable reportId={TEST_ID} />);
        }

        test("renders table header row", () => {
            setup();
            expect(screen.getByRole("row")).toBeInTheDocument();
            expect(screen.getByText("No available data")).toBeInTheDocument();
            expect(screen.getByText("contact us")).toBeInTheDocument();
        });
    });
});
