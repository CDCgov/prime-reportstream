import { describe, test } from "@jest/globals";
import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import { CodeSnippet } from "./CodeSnippet";

describe("CodeSnippet", () => {
    test("default", () => {
        renderApp(<CodeSnippet>Lorem ipsum</CodeSnippet>);
        expect(screen.getByText("Lorem ipsum")).toBeInTheDocument();
    });

    // TEST DEFAULT (INLINE)
    // TEST TOOLTIP = TRUE & FALSE
    // TEST ISBLOCK = TRUE
    // TEST INLINE ISBACKGROUND = TRUE
    // TEST FIGURE
    // TEST FIGURE ISBLOCK = FALSE
    // TEST INLINE ISBUTTON = TRUE
});

export {};
