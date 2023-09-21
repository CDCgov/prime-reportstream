import { screen } from "@testing-library/react";

import { renderApp } from "../../../utils/CustomRenderUtils";

import { FacilitiesProviders } from "./FacilitiesProviders";

describe("FacilitiesProviders", () => {
    test("Breadcrumb displays with link", async () => {
        renderApp(<FacilitiesProviders />);

        const allLinks = screen.getAllByRole("link");
        expect(allLinks[0]).toHaveAttribute("href", "/data-dashboard");
        expect(allLinks[0]).toHaveTextContent("Data Dashboard");
    });
});
