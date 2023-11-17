import { screen } from "@testing-library/react";

import { MemberType } from "../utils/OrganizationUtils";
import { render } from "../utils/Test/render";

import SenderModeBanner from "./SenderModeBanner";

vi.mock("../hooks/UseSenderResource", async (imp) => ({
    ...(await imp<typeof import("../hooks/UseSenderResource")>()),
    default: vi.fn(() => ({
        isLoading: false,
        data: {
            customerStatus: "testing",
        },
    })),
}));

describe("SenderModeBanner", () => {
    test("renders when sender is testing", async () => {
        render(<SenderModeBanner />, {
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
