import { screen } from "@testing-library/react";

import { NoServicesBanner } from "./NoServicesAlert";
import site from "../../content/site.json";
import { renderApp } from "../../utils/CustomRenderUtils";

describe("NoServicesAlert", () => {
    test("displays", () => {
        renderApp(<NoServicesBanner />);

        const link = screen.getByRole("link");
        expect(screen.getByText("No available data")).toBeInTheDocument();
        expect(link).toHaveAttribute("href", site.forms.contactUs.url);
        expect(link).toHaveTextContent("contact us");
    });
});
