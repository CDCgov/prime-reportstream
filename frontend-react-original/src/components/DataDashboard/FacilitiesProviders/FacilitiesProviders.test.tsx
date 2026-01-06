import { screen } from "@testing-library/react";

import { FacilitiesProvidersPage } from "./FacilitiesProviders";
import { renderApp } from "../../../utils/CustomRenderUtils";

describe("FacilitiesProviders", () => {
    test("Breadcrumb displays with link", () => {
        renderApp(<FacilitiesProvidersPage />);

        const allLinks = screen.getAllByRole("link");
        expect(allLinks[0]).toHaveAttribute("href", "/data-dashboard");
        expect(allLinks[0]).toHaveTextContent("Data Dashboard");
    });
});
