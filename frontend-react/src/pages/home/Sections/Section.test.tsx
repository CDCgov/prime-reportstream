import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../../utils/CustomRenderUtils";

import Section from "./Section";

/* REFACTOR
   Is there a better way to handle mocking components when they cause
   issues running simple unit tests?

   >>> Kevin Haube, Oct 12, 2021
*/

describe("Section rendering", () => {
    const fakeSection = {
        title: "Mock title",
        type: "Mock type",
        summary: "Mock summary",
    };

    beforeEach(() => {
        renderWithRouter(<Section section={fakeSection} />);
    });

    test("Section renders props", () => {
        const header = screen.getByTestId("heading");
        const summary = screen.getByTestId("paragraph");

        expect(header).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
        expect(header.innerHTML).toEqual(fakeSection.title);
        expect(summary.innerHTML).toEqual(fakeSection.summary);
    });
});

describe("CTA rendering", () => {
    const fakeCtaSection = {
        title: "Cta title",
        type: "cta",
        summary: "Cta summary",
    };

    beforeEach(() => {
        renderWithRouter(<Section section={fakeCtaSection} />);
    });

    test("Renders <CtaSection /> if type is cta", () => {
        const header = screen.getByTestId("heading");
        const description = screen.getByTestId("description");
        const summary = screen.getByTestId("summary");
        const contactButton = screen.getByRole("button");

        expect(header).toBeInTheDocument();
        expect(description).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
        expect(contactButton).toBeInTheDocument();
    });
});

describe("Live Map rendering", () => {
    const fakeLiveMapSection = {
        title: "Map section",
        type: "liveMap",
        summary: "Map summary",
        description: "Map description",
    };

    beforeEach(() => {
        renderWithRouter(<Section section={fakeLiveMapSection} />);
    });

    test("Renders <LiveMapSection /> if type is liveMap", () => {
        const header = screen.getByTestId("heading");
        const summary = screen.getByTestId("summary");
        const map = screen.getByTestId("map");
        const description = screen.getByTestId("description");

        expect(header).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
        expect(map).toBeInTheDocument();
        expect(description).toBeInTheDocument();
    });
});
