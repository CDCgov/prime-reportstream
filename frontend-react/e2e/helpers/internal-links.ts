import { Page } from "@playwright/test";

export const ELC = "https://www.cdc.gov/epidemiology-laboratory-capacity/php/about/";

export async function clickOnInternalLink(locator: string, dataTestId: string, linkName: string, page: Page) {
    await page.locator(locator).getByTestId(dataTestId).getByRole("link", { name: linkName }).click();
}

export interface SideNavItem {
    name: string;
    path: string;
}

export const aboutSideNav = [
    {
        name: "About",
        path: "/about",
    },
    {
        name: "Our network",
        path: "/about/our-network",
    },
    {
        name: "Product roadmap",
        path: "/about/roadmap",
    },
    {
        name: "News",
        path: "/about/news",
    },
    {
        name: "Case studies",
        path: "/about/case-studies",
    },
    {
        name: "Security",
        path: "/about/security",
    },
    {
        name: "Release notes",
        path: "/about/release-notes",
    },
];

export const gettingStartedSideNav = [
    {
        name: "Getting started",
        path: "/getting-started",
    },
    {
        name: "Sending data",
        path: "/getting-started/sending-data",
    },
    {
        name: "Receiving data",
        path: "/getting-started/receiving-data",
    },
];

export const developerResourcesApiSideNav = [
    {
        name: "ReportStream API",
        path: "/developer-resources/api-onboarding-guide",
    },
    {
        name: "Documentation",
        path: "/developer-resources/documentation",
    },
    {
        name: "Responses from ReportStream",
        path: "/developer-resources/documentation/responses-from-reportstream",
    },
];
