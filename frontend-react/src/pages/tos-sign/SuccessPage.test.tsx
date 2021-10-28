import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { AgreementBody } from "./TermsOfServiceForm";
import SuccessPage from "./SuccessPage";
import site from '../../content/site.json'

describe("Basic rendering", () => {
    const mockData: AgreementBody = {
        title: "",
        firstName: "Kevin",
        lastName: "Haube",
        email: site.orgs.CDC.email,
        territory: "Guam",
        organizationName: "Watermelons",
        operatesInMultipleStates: false,
        agreedToTermsOfService: true
    }

    beforeEach(() => {
        render(<BrowserRouter><SuccessPage data={mockData} /></BrowserRouter>)
    })

    test("Renders without error", async () => {
        const successContainer = await screen.findByTestId("success-container")
        expect(successContainer).toBeInTheDocument()
    })

    test("Data is displayed", () => {
        const name = screen.getByText(`Full name: ${mockData.firstName} ${mockData.lastName}`, { exact: false })
        const email = screen.getByText(`Email: ${mockData.email}`, { exact: false })
        const territory = screen.getByText(`State or territory: ${mockData.territory}`, { exact: false })
        const organizationName = screen.getByText(`Organization name: ${mockData.organizationName}`, { exact: false })

        expect(name).toBeInTheDocument()
        expect(email).toBeInTheDocument()
        expect(territory).toBeInTheDocument()
        expect(organizationName).toBeInTheDocument()
    })

})