import { describe, test } from "@jest/globals";
import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import { IconButton } from "./IconButton";

describe("IconButton", () => {
    test("default", () => {
        renderApp(<IconButton iconProps={{ icon: "Check" }} />);
        expect(screen.getByRole("button")).toBeInTheDocument();
    });

    // TEST ICON
});
