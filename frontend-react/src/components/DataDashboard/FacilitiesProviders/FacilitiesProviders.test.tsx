import { screen } from "@testing-library/react";

import { FacilitiesProvidersPage } from "./FacilitiesProviders";

describe("FacilitiesProviders", () => {
    test("Breadcrumb displays with link", async () => {
        render(<FacilitiesProvidersPage />);

        const allLinks = screen.getAllByRole("link");
        expect(allLinks[0]).toHaveAttribute("href", "/data-dashboard");
        expect(allLinks[0]).toHaveTextContent("Data Dashboard");
    });
});
