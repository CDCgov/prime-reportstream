import { describe, test } from "@jest/globals";
import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import { Button } from "./Button";

describe("Button", () => {
    test("default", () => {
        renderApp(<Button>Test</Button>);
        expect(screen.getByRole("button")).toBeInTheDocument();
    });

    // TEST DEFAULT (type = button)
    // TEST ICON
});
