import { screen } from "@testing-library/react";

import {
    dataDashboardServer,
    makeRSReceiverDeliveryResponseFixture,
    receiverServicesGenerator,
} from "../../../__mocks__/DataDashboardMockServer";
import { mockUseReceiverDeliveries } from "../../../hooks/network/DataDashboard/__mocks__/UseReceiverDeliveries";
import { mockUseOrganizationReceiversFeed } from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";
import { mockFilterManager } from "../../../hooks/filters/mocks/MockFilterManager";
import { renderApp } from "../../../utils/CustomRenderUtils";

import DataDashboardTable from "./DataDashboardTable";

const mockReceiverServices = receiverServicesGenerator(5);
const mockActiveReceiver = mockReceiverServices[0];

vi.mock("../../../TelemetryService", async () => ({
    ...(await vi.importActual<typeof import("../../../TelemetryService")>(
        "../../../TelemetryService",
    )),
}));

vi.mock("../../../contexts/SessionContext");

describe("DataDashboardTable", () => {
    beforeAll(() => dataDashboardServer.listen());
    afterEach(() => dataDashboardServer.resetHandlers());
    afterAll(() => dataDashboardServer.close());

    describe("useOrganizationReceiversFeed without data", () => {
        function setup() {
            // Mock our receiver services feed data
            mockUseOrganizationReceiversFeed.mockReturnValue({
                activeService: undefined,
                isLoading: false,
                data: [],
                setActiveService: () => {},
            } as any);

            // Mock the response from the Deliveries hook
            const mockUseReceiverDeliveriesCallback = {
                data: makeRSReceiverDeliveryResponseFixture(0),
                filterManager: mockFilterManager,
                isLoading: false,
            };
            mockUseReceiverDeliveries.mockReturnValue(
                mockUseReceiverDeliveriesCallback,
            );

            // Render the component
            renderApp(<DataDashboardTable />);
        }

        test("if no active service display NoServicesBanner", async () => {
            setup();
            const heading = await screen.findByText(/No available data/i);
            expect(heading).toBeInTheDocument();
        });
    });
});

describe("DataDashboardTableWithPagination", () => {
    describe("when enabled", () => {
        describe("with multiple receiver services and data", () => {
            function setup() {
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: mockActiveReceiver,
                    isLoading: false,
                    data: mockReceiverServices,
                    setActiveService: () => {},
                } as any);

                const mockUseReceiverDeliveriesCallback = {
                    data: makeRSReceiverDeliveryResponseFixture(10),
                    filterManager: mockFilterManager,
                    isLoading: false,
                };
                mockUseReceiverDeliveries.mockReturnValue(
                    mockUseReceiverDeliveriesCallback,
                );

                // Render the component
                renderApp(<DataDashboardTable />);
            }

            test("renders receiver services", () => {
                setup();
                expect(screen.getAllByRole("option").length).toBe(5);
            });

            test("renders table with pagination", async () => {
                setup();
                const pagination = await screen.findByLabelText(/Pagination/i);
                expect(pagination).toBeInTheDocument();

                // Column headers render
                expect(
                    screen.getByText("Showing all results (101)"),
                ).toBeInTheDocument();
                expect(
                    screen.getByText("Date sent to you"),
                ).toBeInTheDocument();
                expect(
                    screen.getByText("Ordering provider"),
                ).toBeInTheDocument();
                expect(
                    screen.getByText("Performing facility"),
                ).toBeInTheDocument();
                expect(screen.getByText("Submitter")).toBeInTheDocument();
                expect(screen.getByText("Report ID")).toBeInTheDocument();
            });

            test("renders 10 results per page + 1 header row", () => {
                setup();
                // renders 10 results per page + 1 header row regardless of the total number of records
                // since our pagination limit is set to 10
                const rows = screen.getAllByRole("row");
                expect(rows).toHaveLength(10 + 1);
            });
        });

        describe("with one active receiver service", () => {
            function setup() {
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: mockActiveReceiver,
                    data: receiverServicesGenerator(1),
                    setActiveService: () => {},
                } as any);

                const mockUseReceiverDeliveriesCallback = {
                    data: makeRSReceiverDeliveryResponseFixture(0),
                    filterManager: mockFilterManager,
                    isLoading: false,
                };
                mockUseReceiverDeliveries.mockReturnValue(
                    mockUseReceiverDeliveriesCallback,
                );

                // Render the component
                renderApp(<DataDashboardTable />);
            }

            test("renders the receiver service", () => {
                setup();
                expect(screen.queryByRole("select")).not.toBeInTheDocument();
                expect(
                    screen.getByText("Receiver service:"),
                ).toBeInTheDocument();
                expect(screen.getByText("ELR-0")).toBeInTheDocument();
            });

            test("without data", () => {
                setup();
                expect(
                    screen.getByText("No available data"),
                ).toBeInTheDocument();
                expect(screen.getByText("contact us")).toBeInTheDocument();
            });
        });

        describe("with no receiver services", () => {
            function setup() {
                // Mock our receiver services feed data
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: undefined,
                    isLoading: false,
                    data: [],
                    setActiveService: () => {},
                } as any);

                // Mock the response from the Deliveries hook
                const mockUseReceiverDeliveriesCallback = {
                    data: makeRSReceiverDeliveryResponseFixture(0),
                    filterManager: mockFilterManager,
                    isLoading: false,
                };
                mockUseReceiverDeliveries.mockReturnValue(
                    mockUseReceiverDeliveriesCallback,
                );

                // Render the component
                renderApp(<DataDashboardTable />);
            }

            test("renders the NoServicesBanner message", async () => {
                setup();
                const heading = await screen.findByText("No available data");
                expect(heading).toBeInTheDocument();
            });
        });
    });

    describe("when disabled", () => {
        function setup() {
            // Mock our receiver services feed data
            mockUseOrganizationReceiversFeed.mockReturnValue({
                activeService: undefined,
                isLoading: false,
                isDisabled: true,
                data: [],
                setActiveService: () => {},
            } as any);

            // Mock the response from the Deliveries hook
            const mockUseReceiverDeliveriesCallback = {
                data: makeRSReceiverDeliveryResponseFixture(0),
                filterManager: mockFilterManager,
                isLoading: false,
            };
            mockUseReceiverDeliveries.mockReturnValue(
                mockUseReceiverDeliveriesCallback,
            );

            // Render the component
            renderApp(<DataDashboardTable />);
        }

        test("renders an error saying admins shouldn't fetch organization data", async () => {
            setup();
            expect(
                await screen.findByText(
                    "Cannot fetch Organization data as admin",
                ),
            ).toBeVisible();

            expect(
                await screen.findByText("Please try again as an Organization"),
            ).toBeVisible();
        });
    });
});
