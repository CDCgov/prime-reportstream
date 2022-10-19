import { renderHook } from "@testing-library/react-hooks";
import { act, screen } from "@testing-library/react";

import { mockReceiverHook } from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { receiversGenerator } from "../../../network/api/Organizations/Receivers";
import { mockUseOrgDeliveries } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { orgServer } from "../../../__mocks__/OrganizationMockServer";
import { makeDeliveryFixtureArray } from "../../../__mocks__/DeliveriesMockServer";

import DeliveriesTable, { useReceiverFeeds } from "./DeliveriesTable";

describe("DeliveriesTable", () => {
    beforeEach(() => {
        // Mock our SessionProvider's data
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
                senderName: undefined,
            },
            initialized: true,
            dispatch: () => {},
        });
    });
    describe("with services and deliveries data", () => {
        beforeEach(() => {
            // Mock the response from the Receivers hook
            mockReceiverHook.mockReturnValue({
                data: receiversGenerator(3),
                loading: false,
                error: "",
                trigger: () => {},
            });
            // Mock the response from the Deliveries hook
            mockUseOrgDeliveries.mockReturnValue({
                serviceReportsList: makeDeliveryFixtureArray(101),
            });
            // Render the component
            renderWithRouter(<DeliveriesTable />);
        });
        test("renders with no error", async () => {
            // Column headers render
            expect(await screen.findByText("Report ID")).toBeInTheDocument();
            expect(await screen.findByText("Available")).toBeInTheDocument();
            expect(await screen.findByText("Expires")).toBeInTheDocument();
            expect(await screen.findByText("Items")).toBeInTheDocument();
            expect(await screen.findByText("File")).toBeInTheDocument();
        });

        test("renders 100 results per page + 1 header row", () => {
            const rows = screen.getAllByRole("row");
            expect(rows).toHaveLength(100 + 1);
        });
    });
    describe("with no data", () => {
        beforeEach(() => {
            // Mock the response from the Receivers hook
            mockReceiverHook.mockReturnValue({
                data: receiversGenerator(0),
                loading: false,
                error: "",
                trigger: () => {},
            });
            // Mock the response from the Deliveries hook
            mockUseOrgDeliveries.mockReturnValue({
                serviceReportsList: makeDeliveryFixtureArray(0),
            });
            // Render the component
            renderWithRouter(<DeliveriesTable />);
        });
        test("renders 0 results (but 1 header row)", () => {
            const rows = screen.getAllByRole("row");
            expect(rows.length).toBeLessThan(2);
            expect(rows.length).toBeGreaterThan(0);
        });
    });
});

describe("useReceiverFeed", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    beforeEach(() => {
        // Mock our SessionProvider's data
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
                senderName: undefined,
            },
            initialized: true,
            dispatch: () => {},
        });
        mockReceiverHook.mockReturnValue({
            data: receiversGenerator(2),
            error: "",
            loading: false,
            trigger: () => {},
        });
    });
    test("setActive sets an active receiver", async () => {
        const { result } = renderHook(() => useReceiverFeeds());
        expect(result.current.activeService).toEqual({
            name: "elr-0",
            organizationName: "testOrg",
        });
        act(() => result.current.setActiveService(result.current.services[1]));
        expect(result.current.activeService).toEqual({
            name: "elr-1",
            organizationName: "testOrg",
        });
    });
});
