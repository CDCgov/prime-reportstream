import { screen } from "@testing-library/react";

import site from "../../content/site.json";
import { renderApp } from "../../utils/CustomRenderUtils";

import { AgreementBody } from "./TermsOfServiceForm";
import SuccessPage from "./SuccessPage";

describe("Basic rendering", () => {
    const mockData: AgreementBody = {
        title: "",
        firstName: "Kevin",
        lastName: "Haube",
        email: site.orgs.CDC.email,
        territory: "Guam",
        organizationName: "Watermelons",
        operatesInMultipleStates: false,
        agreedToTermsOfService: true,
    };

    beforeEach(() => {
        renderApp(<SuccessPage data={mockData} />);
    });

    test("Renders without error", async () => {
        const successContainer = await screen.findByTestId("success-container");
        expect(successContainer).toBeInTheDocument();
    });

    test("Data is displayed", () => {
        const name = screen.getByText(
            `${mockData.firstName} ${mockData.lastName}`,
            { exact: false },
        );
        const email = screen.getByText(mockData.email, {
            exact: false,
        });
        const territory = screen.getByText(mockData.territory, {
            exact: false,
        });
        const organizationName = screen.getByText(mockData.organizationName, {
            exact: false,
        });

        expect(name).toBeInTheDocument();
        expect(email).toBeInTheDocument();
        expect(territory).toBeInTheDocument();
        expect(organizationName).toBeInTheDocument();
    });
});
