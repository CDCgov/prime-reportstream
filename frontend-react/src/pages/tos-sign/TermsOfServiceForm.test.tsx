import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";

import TermsOfServiceForm from "./TermsOfServiceForm";

describe("Basic rendering", () => {
    beforeEach(() => {
        process.env.REACT_APP_SECRET = "fake secret";
        render(
            <BrowserRouter>
                <TermsOfServiceForm />
            </BrowserRouter>
        );
    });

    it("Title renders", async () => {
        expect(
            await screen.findByText("Account registration")
        ).toBeInTheDocument();
        expect(
            await screen.findByText(
                "Register your organization with ReportStream"
            )
        ).toBeInTheDocument();
    });

    test("Required fields remove error once field is filled in and re-submitted", async () => {
        const DATA = [
            {
                dataTestId: "first-name",
                errorMsg: "First name is a required",
            },
            {
                dataTestId: "last-name",
                errorMsg: "Last name is a required",
            },
            {
                dataTestId: "email",
                errorMsg: "Email is a required field",
            },
            {
                dataTestId: "organization-name",
                errorMsg: "Organization is a required",
            },
        ];
        const submitBtn = screen.getByTestId("submit");

        for (const eachItem of DATA) {
            // clear item, click submit, make sure error is there.
            const inputField = screen.getByTestId(eachItem.dataTestId);

            // fireEvent.change(inputField, {target: {value: " "}});
            fireEvent.change(inputField, { target: { value: "" } });

            userEvent.click(submitBtn);
            expect(
                screen.getByText(eachItem.errorMsg, { exact: false })
            ).toBeInTheDocument();

            // now fill in and see if error messag is cleared
            // fireEvent.change(inputField, {target: {value: "test@example.com"}});
            userEvent.type(inputField, "test@example.com");

            userEvent.click(submitBtn);
            expect(
                screen.queryByText(eachItem.errorMsg, { exact: false })
            ).not.toBeInTheDocument();

            // leave it cleared. If every field has a valid value, then last click on submit button will submit.
            fireEvent.change(inputField, { target: { value: "" } });

            userEvent.click(submitBtn);
            expect(
                screen.getByText(eachItem.errorMsg, { exact: false })
            ).toBeInTheDocument();
        }

        // the agree checkbox is the exception that mucks everything up
        const agreeErrorMsg = "must agree to the Terms of Service";
        const agreedInput = screen.getByTestId("agree");
        userEvent.click(agreedInput);
        expect(agreedInput).toBeChecked();
        userEvent.click(submitBtn);
        // this is "Visible" because it's using a hidden tag unlike other error messages
        // "<ErrorMessage>" vs "<ErrorMessageWithFlag>" .. this drove me nuts writing this test
        expect(
            screen.queryByText(agreeErrorMsg, { exact: false })
        ).not.toBeVisible();

        // toggle it off
        userEvent.click(agreedInput);
        expect(agreedInput).not.toBeChecked();
        userEvent.click(submitBtn);
        expect(
            screen.queryByText(agreeErrorMsg, { exact: false })
        ).toBeVisible();
    });
});
