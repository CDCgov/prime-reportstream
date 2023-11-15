import { screen } from "@testing-library/react";

import { mockUseOrgDeliveries } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { mockFilterManager } from "../../../hooks/filters/mocks/MockFilterManager";
import { receiversGenerator } from "../../../__mocks__/OrganizationMockServer";
import { makeDeliveryFixtureArray } from "../../../__mocks__/DeliveriesMockServer";
import { mockUseOrganizationReceiversFeed } from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";
import { MemberType } from "../../../utils/OrganizationUtils";

import DeliveriesTable from "./DeliveriesTable";

const mockReceivers = receiversGenerator(5);
const mockActiveReceiver = mockReceivers[0];

vi.mock("../../../hooks/UsePagination", async (imp) => ({
    ...(await imp<typeof import("../../../hooks/UsePagination")>()),
    default: vi.fn(() => ({
        currentPageResults: makeDeliveryFixtureArray(10),
        paginationProps: { currentPageNum: 1, slots: [1, 2, 3, 4] },
        isLoading: false,
    })),
}));

const options = {
    providers: {
        Session: {
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
        },
    },
};

describe("DeliveriesTable", () => {
    describe("useReceiverFeed without data", () => {
        function setup() {
            // Mock our receiver services feed data
            mockUseOrganizationReceiversFeed.mockReturnValue({
                activeService: undefined,
                isLoading: false,
                data: [],
                setActiveService: () => {},
                isDisabled: false,
            } as any);

            // Mock the response from the Deliveries hook
            const mockUseOrgDeliveriesCallback = {
                fetchResults: () => Promise.resolve([]),
                filterManager: mockFilterManager,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            render(<DeliveriesTable />, options);
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
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: mockActiveReceiver,
                    isLoading: false,
                    data: mockReceivers,
                    setActiveService: () => {},
                    isDisabled: false,
                } as any);

                const mockUseOrgDeliveriesCallback = {
                    fetchResults: () =>
                        Promise.resolve(makeDeliveryFixtureArray(101)),
                    filterManager: mockFilterManager,
                };
                mockUseOrgDeliveries.mockReturnValue(
                    mockUseOrgDeliveriesCallback,
                );

                // Render the component
                render(<DeliveriesTable />, options);
            }

            test("renders with no error", async () => {
                setup();
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
                setup();
                // renders 10 results per page + 1 header row regardless of the total number of records
                // since our pagination limit is set to 10
                const rows = screen.getAllByRole("row");
                expect(rows).toHaveLength(10 + 1);
            });
        });

        describe("with no services", () => {
            function setup() {
                // Mock our receiver services feed data
                mockUseOrganizationReceiversFeed.mockReturnValue({
                    activeService: undefined,
                    isLoading: false,
                    data: [],
                    setActiveService: () => {},
                    isDisabled: false,
                } as any);

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
                render(<DeliveriesTable />, options);
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
                setActiveService: () => {},
                isDisabled: true,
            } as any);

            // Mock the response from the Deliveries hook
            const mockUseOrgDeliveriesCallback = {
                fetchResults: () =>
                    Promise.resolve(makeDeliveryFixtureArray(0)),
                filterManager: mockFilterManager,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            render(<DeliveriesTable />, options);
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
