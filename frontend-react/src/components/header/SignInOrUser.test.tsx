import { screen } from "@testing-library/react";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { RSSessionContext } from "../../contexts/SessionContext";
import { renderWithSession } from "../../utils/CustomRenderUtils";

import { SignInOrUser } from "./SignInOrUser";

const mockEmail = "mock@abc.com";

describe("SignInOrUser", () => {
    test("renders with email", () => {
        mockSessionContext.mockReturnValueOnce({
            user: {
                email: mockEmail,
            },
        } as RSSessionContext);
        renderWithSession(<SignInOrUser />);
        expect(screen.getByText(mockEmail)).toBeVisible();
    });

    test("renders without email", () => {
        mockSessionContext.mockReturnValueOnce({
            user: {},
        } as RSSessionContext);
        renderWithSession(<SignInOrUser />);
        expect(screen.getByText("unknown user")).toBeVisible();
    });

    test("renders without user", () => {
        mockSessionContext.mockReturnValueOnce({} as RSSessionContext);
        renderWithSession(<SignInOrUser />);
        expect(screen.getByText("Log in via OktaPreview")).toBeVisible();
    });
});
