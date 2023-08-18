import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { ContentSection } from "./ContentSection";

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
        renderApp(<ContentSection {...fakeSection} />);
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
