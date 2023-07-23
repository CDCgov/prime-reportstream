import { screen } from "@testing-library/react";

import { renderApp } from "../../../utils/CustomRenderUtils";

import { FacilitiesProviders } from "./FacilitiesProviders";

describe("FacilitiesProviders", () => {
    test("Breadcrumb displays with link", async () => {
        renderApp(<FacilitiesProviders />);

        const link = screen.getByRole("link");
        expect(link).toHaveAttribute("href", "/data-dashboard");
        expect(link).toHaveTextContent("Data Dashboard");
    });
});
