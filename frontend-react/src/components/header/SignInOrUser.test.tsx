import { screen } from "@testing-library/react";

import { mockSessionContentReturnValue } from "../../contexts/__mocks__/SessionContext";
import { RSSessionContext } from "../../contexts/SessionContext";
import { renderApp } from "../../utils/CustomRenderUtils";

import { SignInOrUser } from "./SignInOrUser";

const mockEmail = "mock@abc.com";

describe("SignInOrUser", () => {
    test("renders with email", () => {
        mockSessionContentReturnValue({
            user: {
                email: mockEmail,
            },
        } as RSSessionContext);
        renderApp(<SignInOrUser />);
        expect(screen.getByText(mockEmail)).toBeVisible();
    });

    test("renders without email", () => {
        mockSessionContentReturnValue({
            user: {},
        } as RSSessionContext);
        renderApp(<SignInOrUser />);
        expect(screen.getByText("unknown user")).toBeVisible();
    });

    test("renders without user", () => {
        mockSessionContentReturnValue({} as RSSessionContext);
        renderApp(<SignInOrUser />);
        expect(screen.getByText("Log in via OktaPreview")).toBeVisible();
    });
});
