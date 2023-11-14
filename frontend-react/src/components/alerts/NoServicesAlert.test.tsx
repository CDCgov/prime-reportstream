import { screen } from "@testing-library/react";

import { render } from "../../utils/CustomRenderUtils";
import site from "../../content/site.json";

import { NoServicesBanner } from "./NoServicesAlert";

describe("NoServicesAlert", () => {
    test("displays", () => {
        render(<NoServicesBanner />);

        const link = screen.getByRole("link");
        expect(screen.getByText("No available data")).toBeInTheDocument();
        expect(link).toHaveAttribute("href", site.forms.contactUs.url);
        expect(link).toHaveTextContent("contact us");
    });
});
