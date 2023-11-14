import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";
import { MemberType } from "../utils/OrganizationUtils";

import SenderModeBanner from "./SenderModeBanner";

vi.mock("../hooks/UseSenderResource", async (imp) => ({
    ...(await imp()),
    useSenderResource: vi.fn(() => ({
        isLoading: false,
        data: {
            customerStatus: "testing",
        },
    })),
}));

describe("SenderModeBanner", () => {
    test("renders when sender is testing", async () => {
        renderApp(<SenderModeBanner />, {
            providers: {
                Session: {
                    activeMembership: {
                        memberType: MemberType.SENDER,
                    },
                },
            },
        });
        const text = await screen.findByText("Learn more about onboarding.");
        expect(text).toBeInTheDocument();
    });
});
