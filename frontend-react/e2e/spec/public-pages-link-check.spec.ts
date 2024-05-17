import { expect, test } from "@playwright/test";

test.describe("Check Links on Public Pages", () => {
    let urlPaths = [];

    test.beforeAll(async ({ browser }) => {
        const page = await browser.newPage();
        const response = await page.goto("/sitemap.xml");
        const sitemapXml = await response!.text();
        urlPaths = await page.evaluate((xmlStr) => {
            const parser = new DOMParser();
            const xmlDoc = parser.parseFromString(xmlStr, "text/xml");
            const urlElements = xmlDoc.querySelectorAll("urlset url loc");
            return Array.from(urlElements).map((elem) =>
                elem.textContent ? new URL(elem.textContent).pathname : null,
            );
        }, sitemapXml);
        await page.close();
    });
    test("Check if paths were fetched", () => {
        expect(urlPaths.length).toBeGreaterThan(0);
    });
});
