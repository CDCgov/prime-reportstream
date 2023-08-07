import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { mockUseOrgDeliveries } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { mockFilterManager } from "../../../hooks/filters/mocks/MockFilterManager";
import {
    orgServer,
    receiversGenerator,
} from "../../../__mocks__/OrganizationMockServer";
import { makeDeliveryFixtureArray } from "../../../__mocks__/DeliveriesMockServer";
import { mockUseOrganizationReceiversFeed } from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";
import { mockAppInsights } from "../../../utils/__mocks__/ApplicationInsights";

import DeliveriesTable from "./DeliveriesTable";

const mockUsePagination = {
    currentPageResults: makeDeliveryFixtureArray(10),
    paginationProps: { currentPageNum: 1, slots: [1, 2, 3, 4] },
    isLoading: false,
};

const mockReceivers = receiversGenerator(5);
const mockActiveReceiver = mockReceivers[0];

jest.mock("../../../hooks/UsePagination", () => ({
    ...jest.requireActual("../../../hooks/UsePagination"),
    default: () => {
        return {
            ...mockUsePagination,
        };
    },
    __esModule: true,
}));

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
            service: "testReceiver",
        },
        dispatch: () => {},
        initialized: true,
        isUserAdmin: false,
        isUserReceiver: true,
        isUserSender: false,
        environment: "test",
    });
});
describe("DeliveriesTable", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    describe("useReceiverFeed without data", () => {
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
            const mockUseOrgDeliveriesCallback = {
                fetchResults: () => Promise.resolve([]),
                filterManager: mockFilterManager,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            renderApp(<DeliveriesTable />);
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

describe("DeliveriesTableWithNumbered", () => {
    describe("when enabled", () => {
        describe("with services and data", () => {
            beforeEach(() => {
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: mockActiveReceiver,
                    loadingServices: false,
                    services: mockReceivers,
                    setActiveService: () => {},
                    isDisabled: false,
                });

                const mockUseOrgDeliveriesCallback = {
                    fetchResults: () =>
                        Promise.resolve(makeDeliveryFixtureArray(101)),
                    filterManager: mockFilterManager,
                };
                mockUseOrgDeliveries.mockReturnValue(
                    mockUseOrgDeliveriesCallback,
                );

                // Render the component
                renderApp(<DeliveriesTable />);
            });

            test("renders with no error", async () => {
                const pagination = await screen.findByLabelText(
                    /Deliveries pagination/i,
                );
                expect(pagination).toBeInTheDocument();
                // Column headers render
                expect(screen.getByText("Report ID")).toBeInTheDocument();
                expect(screen.getByText("Available")).toBeInTheDocument();
                expect(screen.getByText("Expires")).toBeInTheDocument();
                expect(screen.getByText("Items")).toBeInTheDocument();
                expect(screen.getByText("File")).toBeInTheDocument();
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
                        name: "Daily Data | Table Filter",
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

        describe("with no services", () => {
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
                const mockUseOrgDeliveriesCallback = {
                    fetchResults: () =>
                        Promise.resolve(makeDeliveryFixtureArray(0)),
                    filterManager: mockFilterManager,
                };
                mockUseOrgDeliveries.mockReturnValue(
                    mockUseOrgDeliveriesCallback,
                );

                // Render the component
                renderApp(<DeliveriesTable />);
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
            const mockUseOrgDeliveriesCallback = {
                fetchResults: () =>
                    Promise.resolve(makeDeliveryFixtureArray(0)),
                filterManager: mockFilterManager,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            renderApp(<DeliveriesTable />);
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
