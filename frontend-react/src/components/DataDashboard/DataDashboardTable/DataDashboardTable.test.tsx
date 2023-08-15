import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { mockAppInsights } from "../../../utils/__mocks__/ApplicationInsights";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../../hooks/UseOktaMemberships";
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

jest.mock("../../../TelemetryService", () => ({
    ...jest.requireActual("../../../TelemetryService"),
    getAppInsights: () => mockAppInsights,
}));

beforeEach(() => {
    // Mock our SessionProvider's data
    mockSessionContext.mockReturnValue({
        oktaToken: {
            accessToken: "TOKEN",
        },
        activeMembership: {
            memberType: MemberType.RECEIVER,
            parsedName: "testOrg",
            service: "testReceiverService",
        },
        dispatch: () => {},
        initialized: true,
        isUserAdmin: false,
        isUserReceiver: true,
        isUserSender: false,
        environment: "test",
    });
});

describe("DataDashboardTable", () => {
    beforeAll(() => dataDashboardServer.listen());
    afterEach(() => dataDashboardServer.resetHandlers());
    afterAll(() => dataDashboardServer.close());

    describe("useOrganizationReceiversFeed without data", () => {
        beforeEach(() => {
            // Mock our receiver services feed data
            mockUseOrganizationReceiversFeed.mockReturnValue({
                activeService: undefined,
                loadingServices: false,
                services: [],
                setActiveService: () => {},
                isDisabled: false,
            });

            // Mock our SessionProvider's data
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrgNoReceivers",
                    service: "testReceiver",
                },
                dispatch: () => {},
                initialized: true,
                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                environment: "test",
            });

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
        });

        test("if no activeService display NoServicesBanner", async () => {
            const heading = await screen.findByText(
                /Active Services unavailable/i,
            );
            expect(heading).toBeInTheDocument();
            const message = await screen.findByText(
                /No valid receiver found for your organization/i,
            );
            expect(message).toBeInTheDocument();
        });
    });
});

describe("DataDashboardTableWithPagination", () => {
    describe("when enabled", () => {
        describe("with multiple receiver services and data", () => {
            beforeEach(() => {
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: mockActiveReceiver,
                    loadingServices: false,
                    services: mockReceiverServices,
                    setActiveService: () => {},
                    isDisabled: false,
                });

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
            });

            test("renders receiver services", () => {
                expect(screen.getAllByRole("option").length).toBe(5);
            });

            test("renders table with pagination", async () => {
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
                // renders 10 results per page + 1 header row regardless of the total number of records
                // since our pagination limit is set to 10
                const rows = screen.getAllByRole("row");
                expect(rows).toHaveLength(10 + 1);
            });

            describe("TableFilter", () => {
                test("Clicking on filter invokes the trackAppInsightEvent", async () => {
                    await userEvent.click(screen.getByText("Filter"));

                    expect(mockAppInsights.trackEvent).toBeCalledWith({
                        name: "Data Dashboard | Table Filter",
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

        describe("with one receiver service", () => {
            beforeEach(() => {
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: mockActiveReceiver,
                    loadingServices: false,
                    services: receiverServicesGenerator(1),
                    setActiveService: () => {},
                    isDisabled: false,
                });

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
            });

            test("renders the receiver service", () => {
                expect(screen.queryByRole("select")).not.toBeInTheDocument();
                expect(
                    screen.getByText("Receiver service:"),
                ).toBeInTheDocument();
                expect(screen.getByText("ELR-0")).toBeInTheDocument();
            });

            test("without data", () => {
                expect(screen.getByText("No data to show")).toBeInTheDocument();
            });
        });

        describe("with no receiver services", () => {
            beforeEach(() => {
                // Mock our receiver services feed data
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: undefined,
                    loadingServices: false,
                    services: [],
                    setActiveService: () => {},
                    isDisabled: false,
                });

                // Mock our SessionProvider's data
                mockSessionContext.mockReturnValue({
                    oktaToken: {
                        accessToken: "TOKEN",
                    },
                    activeMembership: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrgNoReceivers",
                        service: "testReceiver",
                    },
                    dispatch: () => {},
                    initialized: true,
                    isUserAdmin: false,
                    isUserReceiver: true,
                    isUserSender: false,
                    environment: "test",
                });

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
            });

            test("renders the NoServicesBanner message", async () => {
                const heading = await screen.findByText(
                    "Active Services unavailable",
                );
                expect(heading).toBeInTheDocument();

                const message = await screen.findByText(
                    "No valid receiver found for your organization",
                );
                expect(message).toBeInTheDocument();
            });
        });
    });

    describe("when disabled", () => {
        beforeEach(() => {
            // Mock our receiver services feed data
            mockUseOrganizationReceiversFeed.mockReturnValue({
                activeService: undefined,
                loadingServices: false,
                services: [],
                setActiveService: () => {},
                isDisabled: true,
            });

            // Mock our SessionProvider's data
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrgNoReceivers",
                    service: "testReceiver",
                },
                dispatch: () => {},
                initialized: true,
                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                environment: "test",
            });

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
        });

        test("renders an error saying admins shouldn't fetch organization data", async () => {
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
