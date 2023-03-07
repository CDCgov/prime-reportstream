import { screen } from "@testing-library/react";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { RSSessionContext } from "../../contexts/SessionContext";
import { renderApp } from "../../utils/CustomRenderUtils";

import { SignInOrUser } from "./SignInOrUser";

const mockEmail = "mock@abc.com";

describe("SignInOrUser", () => {
    test("renders with email", () => {
        mockSessionContext.mockReturnValue({
            user: {
                email: mockEmail,
            },
        } as RSSessionContext);
        renderApp(<SignInOrUser />);
        expect(screen.getByText(mockEmail)).toBeVisible();
    });

    test("renders without email", () => {
        mockSessionContext.mockReturnValue({
            user: {},
        } as RSSessionContext);
        renderApp(<SignInOrUser />);
        expect(screen.getByText("unknown user")).toBeVisible();
    });

    test("renders without user", () => {
        mockSessionContext.mockReturnValue({} as RSSessionContext);
        renderApp(<SignInOrUser />);
        expect(screen.getByText("Log in via OktaPreview")).toBeVisible();
    });
});
