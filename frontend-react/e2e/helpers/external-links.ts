import { Page } from "@playwright/test";

export const SIMPLEREPORT = "https://www.simplereport.gov/";
export const RADX_MARS =
    "https://www.nibib.nih.gov/covid-19/radx-tech-program/mars";
export const MAKE_MY_TEST_COUNT = "https://learn.makemytestcount.org/";
export async function clickOnExternalLink(
    locator: string,
    linkName: string,
    page: Page,
) {
    const newTabPromise = page.waitForEvent("popup");
    await page.locator(locator).getByRole("link", { name: linkName }).click();

    const newTab = await newTabPromise;
    await newTab.waitForLoadState();
    return newTab;
}
