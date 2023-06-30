import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import Hero from "./Hero";

describe("Hero rendering", () => {
    beforeEach(() => {
        renderApp(<Hero />);
    });

    test("Title and Summary render on Hero", () => {
        expect(screen.getByRole("banner")).toBeInTheDocument();
    });
});
