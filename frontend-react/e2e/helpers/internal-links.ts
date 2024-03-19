import { Page } from "@playwright/test";

export const ELC = "https://www.cdc.gov/elc/elc-overview.html";

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
