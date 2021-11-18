import { fireEvent, render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";

import site from "../../content/site.json";

import TermsOfServiceForm from "./TermsOfServiceForm";

describe("Basic rendering", () => {
    beforeEach(() => {
        render(
            <BrowserRouter>
                <TermsOfServiceForm />
            </BrowserRouter>
        );
    });

    test("Title renders", () => {
        const preTitle = screen.getByText("Account registration");
        const title = screen.getByText(
            "Register your organization with ReportStream"
        );

        expect(preTitle).toBeInTheDocument();
        expect(title).toBeInTheDocument();
    });

    /* INFO:
    FormGroup, Label, TextInput, Dropdown, and Checkbox, and Button rendering tests should be 
    handled by the trussworks/USWDS component library */

    test("Required fields show error when you submit them as empty", () => {
        fireEvent.click(screen.getByText("Submit registration"));
        expect(
            screen.getByText("First name is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText("Last name is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText("Email is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText("Organization is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText(
                "You must agree to the Terms of Service before using ReportStream"
            )
        ).toBeInTheDocument();
    });

    test("Required fields remove error once field is filled in and re-submitted", () => {
        const button = screen.getByText("Submit registration");
        fireEvent.click(button);

        expect(
            screen.getByText("First name is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText("Last name is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText("Email is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText("Organization is a required field")
        ).toBeInTheDocument();
        expect(
            screen.getByText(
                "You must agree to the Terms of Service before using ReportStream"
            )
        ).toBeInTheDocument();

        const firstNameInput = screen.getByAltText("First name input");
        const lastNameInput = screen.getByAltText("Last name input");
        const emailInput = screen.getByAltText("Email input");
        const organizationInput = screen.getByAltText("Organization input");
        const agreedInput = screen.getByAltText("Agreed checkbox");

        fireEvent.change(firstNameInput, { target: { value: "Kevin" } });
        fireEvent.change(lastNameInput, { target: { value: "Haube" } });
        fireEvent.change(emailInput, {
            target: { value: site.orgs.CDC.email },
        });
        fireEvent.change(organizationInput, { target: { value: "Foobar" } });
        fireEvent.click(agreedInput);
        fireEvent.click(button);

        expect(
            screen.queryByAltText("First name is a required field")
        ).toBeFalsy();
        expect(
            screen.queryByAltText("Last name is a required field")
        ).toBeFalsy();
        expect(screen.queryByAltText("Email is a required field")).toBeFalsy();
        expect(
            screen.queryByAltText("Organization is a required field")
        ).toBeFalsy();
        expect(
            screen.queryByAltText(
                "You must agree to the Terms of Service before using ReportStream"
            )
        ).toBeFalsy();
    });
});
