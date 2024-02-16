import { expect, Page } from "@playwright/test";

export async function clickOnConnect(
    locator: string,
    linkName: string,
    page: Page,
) {
    const newTabPromise = page.waitForEvent("popup");
    await page.locator(locator).getByRole("link", { name: linkName }).click();

    const newTab = await newTabPromise;
    await newTab.waitForLoadState();
    await expect(newTab).toHaveURL(
        "https://app.smartsheetgov.com/b/form/48f580abb9b440549b1a9cf996ba6957",
    );
    expect(newTab.getByText("Connect with ReportStream")).toBeTruthy();
}
