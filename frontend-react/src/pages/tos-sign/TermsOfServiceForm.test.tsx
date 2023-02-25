import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";

import TermsOfServiceForm from "./TermsOfServiceForm";

// FUTURE_TODO: Test submission separately once component refactored
// to not require form submission to validate fields.
describe("Basic rendering", () => {
    const mockSubmit = jest.fn(() => Promise.resolve());
    beforeEach(() => {
        renderApp(<TermsOfServiceForm onSubmit={mockSubmit} />);
    });

    test("Title renders", async () => {
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
                validValue: "John",
            },
            {
                dataTestId: "last-name",
                validValue: "Doe",
            },
            {
                dataTestId: "email",
                validValue: "test@example.com",
            },
            {
                dataTestId: "organization-name",
                validValue: "Contoso",
            },
            {
                dataTestId: "agree",
                validValue: true,
            },
        ];
        const formEle = screen.getByRole("form");
        formEle.onsubmit = () => {};
        const submitBtn = screen.getByTestId("submit");
        userEvent.click(submitBtn);

        await waitFor(() =>
            expect(screen.getAllByRole("alert")).toHaveLength(DATA.length)
        );

        for (const [i, { dataTestId, validValue }] of DATA.entries()) {
            const inputField = screen.getByTestId<HTMLInputElement>(dataTestId);

            // now fill in and see if error message is cleared
            if (inputField.type !== "checkbox") {
                userEvent.type(inputField, validValue.toString());
                await waitFor(() =>
                    expect(inputField).toHaveValue(validValue.toString())
                );
            } else {
                userEvent.click(inputField);
                await waitFor(() => expect(inputField).toBeChecked());
            }
            userEvent.click(submitBtn);

            await waitFor(() =>
                expect(screen.queryAllByRole("alert")).toHaveLength(
                    DATA.length - (i + 1)
                )
            );
        }
    });
});
