import { screen } from "@testing-library/react";

import { render } from "../../utils/CustomRenderUtils";

import Alert, { getAriaRole } from "./Alert";
import type { AlertProps } from "./Alert";

describe("Alert", () => {
    test("renders with defaults (div heading wrap, div body wrap)", () => {
        render(
            <Alert type="info" heading="Heading Test">
                Test
            </Alert>,
        );
        const alert = screen.getByTestId("alert");
        const body = screen.getByText("Test");
        const heading = screen.getByText("Heading Test");
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent("Test");
        expect(screen.queryByRole("heading")).not.toBeInTheDocument();
        expect(heading).toBeInTheDocument();
        expect(heading.tagName.toLowerCase()).toEqual("div");
        expect(body).toBeInTheDocument();
        expect(body.tagName.toLowerCase()).toEqual("div");
    });

    test.each([
        "info",
        "warning",
        "tip",
        "error",
        "success",
    ] as AlertProps["type"][])(
        "renders with aria role for type: %s",
        (type) => {
            render(<Alert type={type}>Test</Alert>);
            expect(screen.getByRole(getAriaRole(type))).toBeInTheDocument();
        },
    );

    test("renders with headingLevel", () => {
        render(
            <Alert type="info" headingLevel="h2" heading="Heading Test">
                Test
            </Alert>,
        );
        const heading = screen.getByRole("heading");
        expect(heading).toBeInTheDocument();
        expect(heading).toHaveTextContent("Heading Test");
        expect(heading.tagName.toLowerCase()).toEqual("h2");
    });
});
