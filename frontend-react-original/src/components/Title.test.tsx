import { screen } from "@testing-library/react";

import Title from "./Title";
import { renderApp } from "../utils/CustomRenderUtils";

describe("Title component", () => {
    const UNIQUE_TITLE = `Title for test`;
    const UNIQUE_PRETITLE = `Unique PreTitle for unit test`;

    it("verify title shows", async () => {
        renderApp(<Title title={UNIQUE_TITLE} preTitle={UNIQUE_PRETITLE} />);
        expect(await screen.findByText(UNIQUE_TITLE)).toBeInTheDocument();
        expect(await screen.findByText(UNIQUE_PRETITLE)).toBeInTheDocument();
    });
});
