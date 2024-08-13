import { expect, Page } from "@playwright/test";

export async function clickOnHome(page: Page) {
    await page.getByTestId("header").getByTitle("Home").click();
    await expect(page).toHaveURL("/");
}

export async function clickOnAbout(page: Page) {
    await page.getByTestId("header").getByTestId("navDropDownButton").getByText("About").click();

    expect(page.getByText("About ReportStream")).toBeTruthy();
    expect(page.getByText("Our network")).toBeTruthy();
    expect(page.getByText("News")).toBeTruthy();
    expect(page.getByText("Case studies")).toBeTruthy();
    expect(page.getByText("Security")).toBeTruthy();
    expect(page.getByText("Release notes")).toBeTruthy();
}

export async function clickOnGettingStarted(page: Page) {
    await page.getByTestId("header").getByRole("link", { name: "Getting started" }).click();

    await expect(page).toHaveURL(/getting-started/);
}

export async function clickOnDevelopers(page: Page) {
    await page.getByTestId("header").getByRole("link", { name: "Developers" }).click();
    await expect(page).toHaveURL(/.*developer-resources/);
}

export async function clickOnYourConnection(page: Page) {
    await page.getByTestId("header").getByRole("link", { name: "Your Connection" }).click();
    await expect(page).toHaveURL(/.*managing-your-connection/);
}

export async function clickOnSupport(page: Page) {
    await page.getByTestId("header").getByRole("link", { name: "Support" }).click();
    await expect(page).toHaveURL(/.*support/);
}
