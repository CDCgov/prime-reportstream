import { Page } from "@playwright/test";

export const CONNECT_URL =
    "https://app.smartsheetgov.com/b/form/48f580abb9b440549b1a9cf996ba6957";
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
