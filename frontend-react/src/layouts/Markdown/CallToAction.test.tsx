import { screen, within } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { CallToAction, CallToActionItem } from "./CallToAction";

describe("CallToAction", () => {
    const ctaItem: CallToActionItem = {
        href: "/",
        label: "Test",
        icon: "Cloud",
        style: "outline",
    };
    test("renders", () => {
        renderApp(<CallToAction {...ctaItem} />);
        const cta = screen.getByRole("link");
        const icon = within(cta).getByRole("img");
        expect(icon).toBeInTheDocument();
        expect(cta).toBeInTheDocument();
        expect(cta).toHaveTextContent("Test");
        expect(cta).toHaveClass("usa-button--outline");
    });
});
