import { screen } from "@testing-library/react";
import { act, renderHook } from "@testing-library/react-hooks";

import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import {
    MembershipController,
    MemberType,
} from "../../../hooks/UseOktaMemberships";
import { mockDeliveryListHook } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { mockReceiverHook } from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";
import { orgServer } from "../../../__mocks__/OrganizationMockServer";
import { deliveriesGenerator } from "../../../network/api/History/Reports";
import { receiversGenerator } from "../../../network/api/Organizations/Receivers";

import ReportsTable, { useReceiverFeeds } from "./ReportsTable";

describe("ReportsTable", () => {
    beforeEach(() => {
        // Mock our SessionProvider's data
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrg",
                        senderName: undefined,
                    },
                },
            } as MembershipController,
        });
        // Mock the response from the Receivers hook
        mockReceiverHook.mockReturnValue({
            data: receiversGenerator(3),
            loading: false,
            error: "",
            trigger: () => {},
        });
        // Mock the response from the Deliveries hook
        mockDeliveryListHook.mockReturnValue({
            data: deliveriesGenerator(101),
            loading: false,
            error: "",
            trigger: () => {},
        });
        // Render the component
        renderWithRouter(<ReportsTable />);
    });

    test("renders with no error", async () => {
        // Column headers render
        expect(await screen.findByText("Report ID")).toBeInTheDocument();
        expect(await screen.findByText("Date Sent")).toBeInTheDocument();
        expect(await screen.findByText("Expires")).toBeInTheDocument();
        expect(await screen.findByText("Total Tests")).toBeInTheDocument();
        expect(await screen.findByText("File")).toBeInTheDocument();
    });

    test("renders 100 results per page + 1 header row", () => {
        const rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(100 + 1);
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
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrg",
                        senderName: undefined,
                    },
                },
            } as MembershipController,
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
