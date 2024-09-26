/* eslint-disable playwright/no-networkidle */
import axios, { AxiosError } from "axios";
import * as fs from "fs";
import { test as baseTest, expect } from "../../test";

const test = baseTest.extend({});

// To save bandwidth, this test is within the /spec/chromium-only/ folder
// Since we're just checking link validity. This is specified within our
// Playwright.config here:
// {
//   name: "chromium-only",
//   use: { browserName: "chromium" },
//   testMatch: "spec/chromium-only/*.spec.ts",
// },


test.describe("Evaluate links on public facing pages", { tag: "@warning" }, () => {
    let urlPaths: string[] = [];
    const normalizeUrl = (href: string, baseUrl: string) => new URL(href, baseUrl).toString();

    // Using our sitemap.xml, we'll create a pathnames array
    // We cannot use our POM, we must
    // create context manually with browser.newContext()
    test.beforeAll(async ({ browser }) => {
        const page = await browser.newPage();
        const response = await page.goto("/sitemap.xml");
        const sitemapXml = await response!.text();
        // Since we don't want to use any external XML parsing libraries,
        // we can use page.evaluate, but that creates it's own execution context
        // wherein we need to explicitly return something, which is why
        // we have the convoluted
        // elem.textContent ? new URL(elem.textContent).pathname : null,
        // combined with the .filter for null values
        urlPaths = await page.evaluate((xmlStr) => {
            const parser = new DOMParser();
            const xmlDoc = parser.parseFromString(xmlStr, "text/xml");
            const urlElements = xmlDoc.querySelectorAll("urlset url loc");
            return Array.from(urlElements)
                .map((elem) => (elem.textContent ? new URL(elem.textContent).pathname : null))
                .filter((path) => path !== null);
        }, sitemapXml);
        await page.close();
    });

    test("Check if paths were fetched", () => {
        expect(urlPaths.length).toBeGreaterThan(0);
    });

    test("Check all public-facing URLs and their links for a valid 200 response", async ({
        page,
        frontendWarningsLogPath,
        isFrontendWarningsLog,
    }) => {
        let aggregateHref = [];
        // Set test timeout to be 1 minute instead of 30 seconds
        test.setTimeout(60000);
        for (const path of urlPaths) {
            await page.goto(path, {
                waitUntil: "networkidle",
            });
            const baseUrl = new URL(page.url()).origin;

            const allATags = await page.getByRole("link", { includeHidden: true }).elementHandles();

            for (const aTag of allATags) {
                const href = await aTag.getAttribute("href");
                // ONLY include http, https and relative path names
                if (href && /^(https?:|\/)/.test(href)) {
                    aggregateHref.push(normalizeUrl(href, baseUrl));
                }
            }
        }
        // Remove any link duplicates to save resources
        aggregateHref = [...new Set(aggregateHref)];

        const axiosInstance = axios.create({
            timeout: 10000,
        });

        const warnings: { url: string; message: string }[] = [];

        const validateLink = async (url: string) => {
            try {
                const response = await axiosInstance.get(url);
                return { url, status: response.status };
            } catch (error) {
                const e = error as AxiosError;
                console.error(`Error accessing ${url}:`, e.message);
                const warning = { url: url, message: e.message };
                warnings.push(warning);

                return {
                    url,
                    status: e.response ? e.response.status : "Request failed",
                };
            }
        };

        const results = await Promise.all(aggregateHref.map((href) => validateLink(href)));

        if (isFrontendWarningsLog && warnings.length > 0) {
            fs.writeFileSync(frontendWarningsLogPath, `${JSON.stringify(warnings)}\n`);
        }

        results.forEach((result) => {
            try {
                expect(result.status).toBe(200);
            } catch (error) {
                const e = error as AxiosError;
                console.warn(`Non-fatal: ${e.message}`);
            }
        });
    });
});
