import { screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import { DailyData } from "./DailyData";
import { makeRSDeliveryHistoryResponseFixture } from "../../../__mockServers__/DeliveriesHistoryMockServer";
import { orgServer, receiversGenerator } from "../../../__mockServers__/OrganizationMockServer";

import useDeliveriesHistory from "../../../hooks/api/deliveries/UseDeliveriesHistory/UseDeliveriesHistory";
import useOrganizationReceivers from "../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers";
import { filterManagerFixture } from "../../../hooks/filters/filters.fixtures";
import { FilterManager } from "../../../hooks/filters/UseFilterManager/UseFilterManager";
import useAppInsightsContext from "../../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";
import { selectDatesFromRange } from "../../../utils/TestUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../contexts/Session/__mocks__/useSessionContext")
>("../../../contexts/Session/useSessionContext");

const mockReceivers = receiversGenerator(5);
const mockActiveReceiver = mockReceivers[0];

const mockFilterManager: FilterManager = {
    ...filterManagerFixture,
    rangeSettings: { from: "2024-03-01", to: "2024-03-30" },
};

vi.mock("../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers");
vi.mock("../../../hooks/api/deliveries/UseDeliveriesHistory/UseDeliveriesHistory");

const mockUseOrganizationReceivers = vi.mocked(useOrganizationReceivers);
const mockUseOrgDeliveries = vi.mocked(useDeliveriesHistory);
const mockUseAppInsightsContext = vi.mocked(useAppInsightsContext);
const { trackEvent } = mockUseAppInsightsContext();
const mockTrackEvent = vi.mocked(trackEvent);

beforeEach(() => {
    // Mock our SessionProvider's data
    mockSessionContentReturnValue({
        authState: {
            accessToken: { accessToken: "TOKEN" },
        } as any,
        activeMembership: {
            memberType: MemberType.RECEIVER,
            parsedName: "testOrg",
            service: "testReceiver",
        },

        user: {
            isUserAdmin: false,
            isUserReceiver: true,
            isUserSender: false,
            isUserTransceiver: false,
        } as any,
    });
});
describe("DeliveriesTable", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    describe("useReceiverFeed without data", () => {
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
            const mockUseOrgDeliveriesCallback = {
                data: makeRSDeliveryHistoryResponseFixture(10),
                filterManager: mockFilterManager,
                searchTerm: "",
                setSearchTerm: () => Promise.resolve([]),
                setService: () => Promise.resolve([]),
                dataUpdatedAt: 111,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            renderApp(<DailyData />);
        }

        test("if no activeService display NoServicesBanner", async () => {
            setup();
            const heading = await screen.findByText(/No available data/i);
            expect(heading).toBeInTheDocument();
        });
    });
});

describe("DeliveriesTableWithNumbered", () => {
    describe("when enabled", () => {
        describe("with active services and data", () => {
            function setup() {
                mockUseOrganizationReceivers.mockReturnValue({
                    allReceivers: [mockActiveReceiver],
                    activeReceivers: [mockActiveReceiver],
                    isLoading: false,
                    isDisabled: false,
                } as any);

                const mockUseOrgDeliveriesCallback = {
                    data: makeRSDeliveryHistoryResponseFixture(10),
                    filterManager: mockFilterManager,
                    searchTerm: "",
                    setSearchTerm: () => Promise.resolve([]),
                    setService: () => Promise.resolve([]),
                    dataUpdatedAt: 111,
                };
                mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

                // Render the component
                renderApp(<DailyData />);
            }

            test("renders with no error", async () => {
                setup();
                const pagination = await screen.findByLabelText("Pagination");

                expect(pagination).toBeInTheDocument();
                // Column headers render
                expect(screen.getByText("Report ID")).toBeInTheDocument();
                expect(screen.getByText("Time received")).toBeInTheDocument();
                expect(screen.getByText("File available until")).toBeInTheDocument();
                expect(screen.getByText("Items")).toBeInTheDocument();
                expect(screen.getByText("Filename")).toBeInTheDocument();
            });

            test("renders 10 results per page + 1 header row", () => {
                setup();
                // renders 10 results per page + 1 header row regardless of the total number of records
                // since our pagination limit is set to 10
                const rows = screen.getAllByRole("row");
                expect(rows).toHaveLength(10 + 1);
            });

            describe.skip("TableFilter", () => {
                test("Clicking on Apply invokes the trackAppInsightEvent", async () => {
                    setup();
                    await selectDatesFromRange("20", "23");
                    await userEvent.click(screen.getByText("Apply"));

                    expect(mockTrackEvent).toHaveBeenCalledWith({
                        name: "Daily Data | Table Filter",
                        properties: {
                            tableFilter: {
                                endRange: "2024-03-23T23:59:00.000Z",
                                startRange: "2024-03-20T00:00:00.000Z",
                            },
                        },
                    });
                });
            });
        });

        describe("with no services", () => {
            function setup() {
                // Mock our receiver services feed data
                mockUseOrganizationReceivers.mockReturnValue({
                    allReceivers: [mockActiveReceiver],
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
                const mockUseOrgDeliveriesCallback = {
                    data: makeRSDeliveryHistoryResponseFixture(10),
                    filterManager: mockFilterManager,
                    searchTerm: "",
                    setSearchTerm: () => Promise.resolve([]),
                    setService: () => Promise.resolve([]),
                    dataUpdatedAt: 111,
                };
                mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

                // Render the component
                renderApp(<DailyData />);
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
            const mockUseOrgDeliveriesCallback = {
                data: makeRSDeliveryHistoryResponseFixture(10),
                filterManager: mockFilterManager,
                searchTerm: "",
                setSearchTerm: () => Promise.resolve([]),
                setService: () => Promise.resolve([]),
                dataUpdatedAt: 111,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            renderApp(<DailyData />);
        }

        test("renders an error saying admins shouldn't fetch organization data", async () => {
            setup();
            expect(await screen.findByText("Cannot fetch Organization data as admin")).toBeVisible();

            expect(await screen.findByText("Please try again as an Organization")).toBeVisible();
        });
    });
});
