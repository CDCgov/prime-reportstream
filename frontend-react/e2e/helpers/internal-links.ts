import { expect, Page } from "@playwright/test";

export const ELC = "https://www.cdc.gov/elc/elc-overview.html";
export const NIST =
    "https://www.cdc.gov/vaccines/programs/iis/technical-guidance/downloads/hl7guide-1-5-2014-11.pdf";

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
