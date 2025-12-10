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

    it("should render APHL AIMS platform link", () => {
        render(<SunsetNoticeBanner />);
        const link = screen.getByRole("link", {
            name: /Association of Public Health Laboratories' \(APHL\) Informatics Messaging System \(AIMS\)/i,
        });
        expect(link).toHaveAttribute("href", "https://www.aphl.org/programs/informatics/pages/aims_platform.aspx");
    });

    it("should render AIMS Platform Customer Portal link", () => {
        render(<SunsetNoticeBanner />);
        const link = screen.getByRole("link", {
            name: /AIMS Platform Customer Portal/i,
        });
        expect(link).toHaveAttribute(
            "href",
            "https://aphlinformatics.atlassian.net/servicedesk/customer/portal/23/",
        );
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
