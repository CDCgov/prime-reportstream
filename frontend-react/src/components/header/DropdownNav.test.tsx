import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { HowItWorksDropdown, GettingStartedDropdown } from "./DropdownNav";

test("How It Works Dropdown", () => {
    renderWithRouter(<HowItWorksDropdown />);
    expect(screen.getByText("About")).toBeInTheDocument();
    expect(screen.getByText("Where we're live")).toBeInTheDocument();
    expect(screen.getByText("System and settings")).toBeInTheDocument();
    expect(screen.getByText("Security practices")).toBeInTheDocument();

    expect(screen.getByText("About")).toHaveAttribute(
        "href",
        "/how-it-works/about"
    );
    expect(screen.getByText("Where we're live")).toHaveAttribute(
        "href",
        "/how-it-works/where-were-live"
    );
    expect(screen.getByText("System and settings")).toHaveAttribute(
        "href",
        "/how-it-works/system-and-settings"
    );
    expect(screen.getByText("Security practices")).toHaveAttribute(
        "href",
        "/how-it-works/security-practices"
    );
});

test("Getting Started Dropdown", () => {
    renderWithRouter(<GettingStartedDropdown />);
    expect(screen.getByText("Public health departments")).toBeInTheDocument();
    expect(screen.getByText("Testing facilities")).toBeInTheDocument();

    expect(screen.getByText("Public health departments")).toHaveAttribute(
        "href",
        "/getting-started/public-health-departments/overview"
    );
    expect(screen.getByText("Testing facilities")).toHaveAttribute(
        "href",
        "/getting-started/testing-facilities/overview"
    );
});
