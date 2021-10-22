import { fireEvent, render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import SigningForm from "./SigningForm";

describe('Basic rendering', () => {
    const mockCallback = jest.fn();
    beforeEach(() => {
        render(<BrowserRouter><SigningForm signedCallback={mockCallback} /></BrowserRouter>)
    })

    test('Title renders', () => {
        const preTitle = screen.getByText("Account registration")
        const title = screen.getByText("Register your organization with ReportStream")

        expect(preTitle).toBeInTheDocument()
        expect(title).toBeInTheDocument()
    })

    /* INFO:
    FormGroup, Label, TextInput, Dropdown, and Checkbox, and Button rendering tests handled by the
    trussworks/USWDS component library */

    test('Required fields show error when you submit them as empty', () => {
        fireEvent(screen.getByText('Submit registration'), new MouseEvent("click"))
        expect(screen.getByText('First name is a required field')).toBeInTheDocument()
        expect(screen.getByText('Last name is a required field')).toBeInTheDocument()
        expect(screen.getByText('Email is a required field')).toBeInTheDocument()
        expect(screen.getByText('Organization is a required field')).toBeInTheDocument()
        expect(screen.getByText('You must agree to the Terms of Service before using ReportStream')).toBeInTheDocument()
    })
})
