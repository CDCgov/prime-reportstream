import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import Title from "./Title";

describe("Title component", () => {
    const UNIQUE_TITLE = `Title for test`;
    const UNIQUE_PRETITLE = `Unique PreTitle for unit test`;
    beforeEach(() => {
        renderApp(<Title title={UNIQUE_TITLE} preTitle={UNIQUE_PRETITLE} />);
    });

    it("verify title shows", async () => {
        expect(await screen.findByText(UNIQUE_TITLE)).toBeInTheDocument();
        expect(await screen.findByText(UNIQUE_PRETITLE)).toBeInTheDocument();
    });
});
