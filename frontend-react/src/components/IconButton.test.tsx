import { describe, test } from "@jest/globals";
import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import { IconButton } from "./IconButton";

describe("IconButton", () => {
    test("default", () => {
        renderApp(<IconButton iconProps={{ icon: "Check" }} />);
        expect(screen.getByRole("button")).toBeInTheDocument();
        expect(screen.getByRole("img")).toBeInTheDocument();
    });

    test("no icon", () => {
        const args = {} as any;
        expect(() => renderApp(<IconButton {...args} />)).toThrowError(
            "IconButton component requires an icon."
        );
    });
});
