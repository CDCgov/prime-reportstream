import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import {
    MembershipController,
    MemberType,
} from "../../../hooks/UseOktaMemberships";
import { SessionController } from "../../../hooks/UseSessionStorage";
import {
    deliveriesGenerator,
    mockDeliveryListHook,
} from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import {
    mockReceiverHook,
    receiversGenerator,
} from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";

import ReportsTable from "./ReportsTable";

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
            store: {} as SessionController, // TS yells about removing this because of types
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
    test("renders 100 results per page + 1 header row", () => {
        const rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(100 + 1);
    });
});
