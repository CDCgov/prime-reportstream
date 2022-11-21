import { renderHook } from "@testing-library/react-hooks";
import { act, screen } from "@testing-library/react";

import { mockReceiverHook } from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { mockUseOrgDeliveries } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { receiversGenerator } from "../../../network/api/Organizations/Receivers";
import { mockFilterManager } from "../../../hooks/filters/mocks/MockFilterManager";
import { orgServer } from "../../../__mocks__/OrganizationMockServer";
import { makeDeliveryFixtureArray } from "../../../__mocks__/DeliveriesMockServer";

import DeliveriesTable, { useReceiverFeeds } from "./DeliveriesTable";

const mockUsePagination = {
    currentPageResults: makeDeliveryFixtureArray(10),
    paginationProps: { currentPageNum: 1, slots: [1, 2, 3, 4] },
    isLoading: false,
};

jest.mock("../../../hooks/UsePagination", () => ({
    ...jest.requireActual("../../../hooks/UsePagination"),
    default: () => {
        return {
            ...mockUsePagination,
        };
    },
    __esModule: true,
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
            service: "testSender",
        },
        dispatch: () => {},
        initialized: true,
    });
});
describe("DeliveriesTable", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    describe("useReceiverFeed with data", () => {
        beforeEach(() => {
            mockReceiverHook.mockReturnValue({
                data: receiversGenerator(2),
                error: "",
                loading: false,
                trigger: () => {},
            });

            // Mock the response from the Deliveries hook
            const mockUseOrgDeliveriesCallback = {
                fetchResults: () =>
                    Promise.resolve(makeDeliveryFixtureArray(101)),
                filterManager: mockFilterManager,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            renderWithRouter(<DeliveriesTable />);
        });

        test("setActiveService sets an active receiver", async () => {
            const { result } = renderHook(() => useReceiverFeeds());
            expect(result.current.activeService).toEqual({
                name: "elr-0",
                organizationName: "testOrg",
            });
            act(() =>
                result.current.setActiveService(result.current.services[1])
            );
            expect(result.current.activeService).toEqual({
                name: "elr-1",
                organizationName: "testOrg",
            });
        });
    });

    describe("useReceiverFeed without data", () => {
        beforeEach(() => {
            mockReceiverHook.mockReturnValue({
                data: [],
                error: "",
                loading: false,
                trigger: () => {},
            });

            // Mock the response from the Deliveries hook
            const mockUseOrgDeliveriesCallback = {
                fetchResults: () => Promise.resolve([]),
                filterManager: mockFilterManager,
            };
            mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

            // Render the component
            renderWithRouter(<DeliveriesTable />);
        });

        test("if no activeService display NoServicesBanner", async () => {
            const heading = await screen.findByText(
                /Active Services unavailable/i
            );
            expect(heading).toBeInTheDocument();
            const message = await screen.findByText(
                /No valid sender found for your organization/i
            );
            expect(message).toBeInTheDocument();
        });
    });
});

describe("DeliveriesTableWithNumberedPagination - with data", () => {
    beforeEach(async () => {
        // Mock the response from the Receivers hook
        mockReceiverHook.mockReturnValue({
            data: receiversGenerator(3),
            loading: false,
            error: "",
            trigger: () => {},
        });

        // Mock the response from the Deliveries hook
        const mockUseOrgDeliveriesCallback = {
            fetchResults: () => Promise.resolve(makeDeliveryFixtureArray(101)),
            filterManager: mockFilterManager,
        };
        mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

        // Render the component
        renderWithRouter(<DeliveriesTable />);
    });

    test("renders with no error", async () => {
        const pagination = await screen.findByLabelText(
            /Deliveries pagination/i
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
});

describe("DeliveriesTableWithNumberedPagination - with no data", () => {
    beforeEach(() => {
        // Mock the response from the Receivers hook
        mockReceiverHook.mockReturnValue({
            data: receiversGenerator(0),
            loading: false,
            error: "",
            trigger: () => {},
        });

        // Mock the response from the Deliveries hook
        const mockUseOrgDeliveriesCallback = {
            fetchResults: () => Promise.resolve(makeDeliveryFixtureArray(0)),
            filterManager: mockFilterManager,
        };
        mockUseOrgDeliveries.mockReturnValue(mockUseOrgDeliveriesCallback);

        // Render the component
        renderWithRouter(<DeliveriesTable />);
    });

    test("renders the NoServicesBanner message", async () => {
        const heading = await screen.findByText("Active Services unavailable");
        expect(heading).toBeInTheDocument();

        const message = await screen.findByText(
            "No valid sender found for your organization"
        );
        expect(message).toBeInTheDocument();
    });
});
