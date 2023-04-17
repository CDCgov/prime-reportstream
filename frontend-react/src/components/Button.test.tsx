import { describe, test } from "@jest/globals";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../utils/CustomRenderUtils";

import { Button } from "./Button";

describe("Button", () => {
    test("default", () => {
        renderApp(<Button>Test</Button>);
        const button = screen.getByRole("button");
        expect(button).toBeInTheDocument();
        expect(button).toHaveAttribute("type", "button");
    });

    test("custom type", () => {
        renderApp(<Button type="submit">Test</Button>);
        expect(screen.getByRole("button")).toHaveAttribute("type", "submit");
    });

    test("tooltip", async () => {
        const user = userEvent.setup();
        renderApp(
            <Button tooltip={{ tooltipContentProps: { children: "Hello" } }}>
                Test
            </Button>
        );
        const button = screen.getByRole("button");
        expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();

        await user.hover(button);
        const tooltip = screen.getByRole("tooltip");
        expect(tooltip).toBeVisible();
        expect(tooltip).toHaveTextContent("Hello");

        await user.unhover(button);
        expect(tooltip).not.toBeInTheDocument();
    });
});
