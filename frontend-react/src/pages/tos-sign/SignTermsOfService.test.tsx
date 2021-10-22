import { render, screen } from "@testing-library/react";
import SignTermsOfService from "./SignTermsOfService";
import { BrowserRouter } from "react-router-dom";

describe("Basic rendering", () => {

    beforeEach(() => {
        render(<BrowserRouter><SignTermsOfService /></BrowserRouter>)
    })

    test("Renders without error", async () => {
        const container = await screen.findByTestId("container")
        expect(container).toBeInTheDocument()
    })

    test("Signed is false on render so SigningForm is displayed", async () => {
        const formContaienr = await screen.findByTestId("form-container")
        expect(formContaienr).toBeInTheDocument()
    })

})