import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";
import site from "../../content/site.json";

import { NoServicesBanner } from "./NoServicesAlert";

describe("NoServicesAlert", () => {
    test("displays", () => {
        renderApp(<NoServicesBanner />);

        const link = screen.getByRole("link");
        expect(screen.getByText("No available data")).toBeInTheDocument();
        expect(link).toHaveAttribute("href", site.forms.contactUs.url);
        expect(link).toHaveTextContent("contact us");
    });
});
