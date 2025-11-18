import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";

import SunsetNoticeBanner from "./SunsetNoticeBanner";

describe("SunsetNoticeBanner", () => {
    it("should render the sunset notice heading", () => {
        render(<SunsetNoticeBanner />);
        expect(screen.getByText("ReportStream Sunset Notice")).toBeInTheDocument();
    });

    it("should render the sunset date", () => {
        render(<SunsetNoticeBanner />);
        expect(screen.getByText(/December 31, 2025/)).toBeInTheDocument();
    });

    it("should mention AIMS Platform Customer Portal", () => {
        render(<SunsetNoticeBanner />);
        expect(screen.getByText(/AIMS Platform Customer Portal/i)).toBeInTheDocument();
    });

    it("should render email link with subject", () => {
        render(<SunsetNoticeBanner />);
        const emailLink = screen.getByRole("link", { name: /OPHDST@cdc.gov/i });
        expect(emailLink).toHaveAttribute("href", expect.stringContaining("subject=ReportStream%20Sunset"));
    });

    it("should have proper ARIA attributes", () => {
        const { container } = render(<SunsetNoticeBanner />);
        const section = container.querySelector("section");
        expect(section).toHaveAttribute("role", "region");
        expect(section).toHaveAttribute("aria-label", "Site alert");
    });
});
