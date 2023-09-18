import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { NoServicesBanner } from "./NoServicesAlert";

describe("NoServicesAlert", () => {
    test("displays", () => {
        renderApp(<NoServicesBanner />);

        const link = screen.getByRole("link");
        expect(screen.getByText("No available data")).toBeInTheDocument();
        expect(link).toHaveAttribute(
            "href",
            "https://app.smartsheetgov.com/b/form/da894779659b45768079200609b3a599",
        );
        expect(link).toHaveTextContent("contact us");
    });
});
