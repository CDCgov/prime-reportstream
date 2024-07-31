import { Page } from "@playwright/test";

export async function clickNetwork(page: Page) {
    await page
        .getByTestId("sidenav")
        .getByRole("link", { name: /Our network/ })
        .click();
}

export async function clickRoadmap(page: Page) {
    await page
        .getByTestId("sidenav")
        .getByRole("link", { name: /Product roadmap/ })
        .click();
}

export async function clickNews(page: Page) {
    await page
        .getByTestId("sidenav")
        .getByRole("link", { name: /News/ })
        .click();
}

export async function clickCaseStudies(page: Page) {
    await page
        .getByTestId("sidenav")
        .getByRole("link", { name: /Case studies/ })
        .click();
}

export async function clickSecurity(page: Page) {
    await page
        .getByTestId("sidenav")
        .getByRole("link", { name: /Security/ })
        .click();
}

export async function clickReleaseNotes(page: Page) {
    await page
        .getByTestId("sidenav")
        .getByRole("link", { name: /Release notes/ })
        .click();
}
