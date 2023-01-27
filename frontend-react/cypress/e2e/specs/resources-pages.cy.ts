import indexPage from "../../support/pages/resources/Index";
import showPage from "../../support/pages/resources/Show";

describe("Resources Pages", () => {
    context("Index", () => {
        beforeEach(() => {
            indexPage.go();
        });

        it("renders the correct headings", () => {
            indexPage.getElements().heading.should("contain", "Resources");
            indexPage
                .getElements()
                .subheading.should(
                    "contain",
                    "Explore guides, tools, and resources to optimize ReportStream"
                );
        });
    });

    context("Show", () => {
        // TODO: fix Cypress Webpack loader since resources/index imports markdown so we could just import slugs
        // TODO: also add subheading text to resources/index?
        const slugsAndHeadings = [
            {
                slug: "account-registration-guide",
                headingText: "Account registration guide",
                subheadingText:
                    "Follow these steps to set up a new user account with ReportStream.",
            },
            {
                slug: "getting-started-public-health-departments",
                headingText: "Guide to receiving ReportStream data",
                subheadingText:
                    "A step-by-step process for connecting your jurisdiction to ReportStream",
            },
            {
                slug: "getting-started-submitting-data",
                headingText: "Guide to submitting data to ReportStream",
                subheadingText:
                    "A step-by-step process for connecting your lab or facility to ReportStream",
            },
            /*
            // TODO: two h2s -- will come back
            {
                slug: "elr-checklist",
                headingText: "ELR onboarding checklist",
                subheadingText:
                    "If you're a public health department and want to connect ReportStream through Electronic Lab Reporting (ELR), you'll need to fill out the ReportStream ELR onboarding form." +
                    "This checklist provides a preview of what we'll ask, so you can gather everything you need to complete the form.",
            },
*/
            {
                slug: "programmers-guide",
                headingText: "API Programmer's guide",
                subheadingText:
                    "Full documentation for interacting with the ReportStream API",
            },
            {
                slug: "data-download-guide",
                headingText: "Manual data download guide",
                subheadingText:
                    "Instructions for public health departments to download data manually from the ReportStream application.",
            },
            {
                slug: "referral-guide",
                headingText: "How to refer users to ReportStream",
                subheadingText:
                    "This guide provides instructions and communications for referring disease reporting entities to use ReportStream.",
            },
            {
                slug: "system-and-settings",
                headingText: "System and settings",
                subheadingText:
                    "Information about the ReportStream platform, including data configuration, formats, and transport",
            },
            {
                slug: "security-practices",
                headingText: "Security practices",
                subheadingText:
                    "Answers to common questions about ReportStream security and data practices.",
            },
        ];

        it.each(slugsAndHeadings)(
            "renders the correct headings for each slug",
            ({ slug, headingText, subheadingText }) => {
                showPage.go(slug);
                showPage.getElements().heading.should("contain", headingText);
                showPage
                    .getElements()
                    .subheading.should("contain", subheadingText);
            }
        );
    });
});
