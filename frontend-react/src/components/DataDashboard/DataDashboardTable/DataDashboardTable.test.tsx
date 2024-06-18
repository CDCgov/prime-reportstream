import { screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import DataDashboardTable from "./DataDashboardTable";
import {
    dataDashboardServer,
    makeRSReceiverDeliveryResponseFixture,
    receiverServicesGenerator,
} from "../../../__mockServers__/DataDashboardMockServer";
import useReceiverDeliveries from "../../../hooks/api/deliveries/UseReceiverDeliveries/UseReceiverDeliveries";
import useOrganizationReceivers from "../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers";
import { filterManagerFixture } from "../../../hooks/filters/filters.fixtures";
import useAppInsightsContext from "../../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";
import { selectDatesFromRange } from "../../../utils/TestUtils";

vi.mock(
    "../../../hooks/api/deliveries/UseReceiverDeliveries/UseReceiverDeliveries",
);
vi.mock(
    "../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers",
);

const mockUseReceiverDeliveries = vi.mocked(useReceiverDeliveries);
const mockUseOrganizationReceivers = vi.mocked(useOrganizationReceivers);
const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../contexts/Session/__mocks__/useSessionContext")
>("../../../contexts/Session/useSessionContext");
const mockReceiverServices = receiverServicesGenerator(5);
const mockActiveReceiver = mockReceiverServices[0];
const mockUseAppInsightsContext = vi.mocked(useAppInsightsContext);
const mockAppInsights = mockUseAppInsightsContext();
const mockFilterManager = { ...filterManagerFixture };

beforeEach(() => {
    // Mock our SessionProvider's data
    mockSessionContentReturnValue({
        authState: {
            accessToken: { accessToken: "TOKEN" },
        } as any,
        activeMembership: {
            memberType: MemberType.RECEIVER,
            parsedName: "testOrg",
            service: "testReceiverService",
        },

        user: {
            isUserAdmin: false,
            isUserReceiver: true,
            isUserSender: false,
            isUserTransceiver: false,
        } as any,
    });
});

describe("DataDashboardTable", () => {
    beforeAll(() => dataDashboardServer.listen());
    afterEach(() => dataDashboardServer.resetHandlers());
    afterAll(() => dataDashboardServer.close());

    describe("useOrganizationReceivers without data", () => {
        function setup() {
            // Mock our receiver services feed data
            mockUseOrganizationReceivers.mockReturnValue({
                allReceivers: [],
                activeReceivers: [],
                isLoading: false,
                isDisabled: false,
            } as any);

            // Mock our SessionProvider's data
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrgNoReceivers",
                    service: "testReceiver",
                },

                user: {
                    isUserAdmin: false,
                    isUserReceiver: true,
                    isUserSender: false,
                    isUserTransceiver: false,
                } as any,
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
                mockUseOrganizationReceivers.mockReturnValue({
                    allReceivers: [
                        mockActiveReceiver,
                        mockActiveReceiver,
                        mockActiveReceiver,
                        mockActiveReceiver,
                        mockActiveReceiver,
                    ],
                    activeReceivers: [
                        mockActiveReceiver,
                        mockActiveReceiver,
                        mockActiveReceiver,
                        mockActiveReceiver,
                        mockActiveReceiver,
                    ],
                    isLoading: false,
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

            describe("TableFilter", () => {
                test("Clicking on filter invokes the trackAppInsightEvent", async () => {
                    setup();
                    await selectDatesFromRange("20", "23");
                    await userEvent.click(screen.getByText("Filter"));

                    expect(mockAppInsights.trackEvent).toHaveBeenCalledWith({
                        name: "Data Dashboard | Table Filter",
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

        describe("with one active receiver service", () => {
            function setup() {
                mockUseOrganizationReceivers.mockReturnValue({
                    allReceivers: [mockActiveReceiver],
                    activeReceivers: [mockActiveReceiver],
                    isLoading: false,
                    isDisabled: false,
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
                mockUseOrganizationReceivers.mockReturnValue({
                    allReceivers: [],
                    activeReceivers: [],
                    isLoading: false,
                    isDisabled: false,
                } as any);

                // Mock our SessionProvider's data
                mockSessionContentReturnValue({
                    authState: {
                        accessToken: { accessToken: "TOKEN" },
                    } as any,
                    activeMembership: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrgNoReceivers",
                        service: "testReceiver",
                    },

                    user: {
                        isUserAdmin: false,
                        isUserReceiver: true,
                        isUserSender: false,
                        isUserTransceiver: false,
                    } as any,
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
            mockUseOrganizationReceivers.mockReturnValue({
                allReceivers: [],
                activeReceivers: [],
                isLoading: false,
                isDisabled: true,
            } as any);

            // Mock our SessionProvider's data
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrgNoReceivers",
                    service: "testReceiver",
                },

                user: {
                    isUserAdmin: false,
                    isUserReceiver: true,
                    isUserSender: false,
                    isUserTransceiver: false,
                } as any,
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
