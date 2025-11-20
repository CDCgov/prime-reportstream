import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

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
        render(<SunsetNoticeBanner />);
        const section = screen.getByRole("region", { name: "Site alert" });
        expect(section).toBeInTheDocument();
        expect(section).toHaveAttribute("aria-label", "Site alert");
    });
});
