import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";

import { FacilitiesProvidersPage } from "./FacilitiesProviders";

vi.mock("./FacilitiesProvidersTable");

describe("FacilitiesProviders", () => {
    test("Breadcrumb displays with link", async () => {
        render(<FacilitiesProvidersPage />);

        const allLinks = screen.getAllByRole("link");
        expect(allLinks[0]).toHaveAttribute("href", "/data-dashboard");
        expect(allLinks[0]).toHaveTextContent("Data Dashboard");
    });

    test("if no active service display NoServicesBanner", async () => {
        render(<FacilitiesProvidersPage />);
        const heading = await screen.findByText(/No available data/i);
        expect(heading).toBeInTheDocument();
    });
});
