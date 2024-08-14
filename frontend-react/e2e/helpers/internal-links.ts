import { Page } from "@playwright/test";

export const ELC =
    "https://www.cdc.gov/epidemiology-laboratory-capacity/php/about/";

export async function clickOnInternalLink(
    locator: string,
    dataTestId: string,
    linkName: string,
    page: Page,
) {
    await page
        .locator(locator)
        .getByTestId(dataTestId)
        .getByRole("link", { name: linkName })
        .click();
}
