import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import content from "./content.json";
import Home from "./Home";

describe("Home rendering", () => {
    beforeEach(() => {
        renderApp(<Home />);
    });

    test("Container renders", () => {
        expect(screen.getByTestId("container-get-started")).toBeInTheDocument();
        expect(
            screen.getByTestId("container-how-it-works"),
        ).toBeInTheDocument();
        expect(
            screen.getByTestId("container-other-products"),
        ).toBeInTheDocument();
        expect(screen.getByTestId("container-map")).toBeInTheDocument();
        expect(
            screen.getByTestId("container-other-partners"),
        ).toBeInTheDocument();
        expect(screen.getByTestId("container-connect")).toBeInTheDocument();
    });

    test("Renders correct number of elements", async () => {
        // these tests require each new feature section has a `data-testid="feature"` set!

        // note: forEach() is not async friendly
        for (const eachSection of content.sections) {
            for (const eachFeature of eachSection.features ?? []) {
                // make sure each feature for each section appears somewhere on the page.
                expect(await screen.findByText(eachFeature.title)).toBeValid();
            }
        }

        expect(await screen.findAllByTestId("section")).toHaveLength(
            content.sections.length +
                content.otherProducts.length +
                content.liveMapContact.length +
                content.otherPartners.length,
        );
    });
});
