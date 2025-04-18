/* eslint-disable playwright/no-networkidle */
import axios, { AxiosError } from "axios";
import * as fs from "fs";
import { pageNotFound } from "../../../src/content/error/ErrorMessages";
import { isAbsoluteURL, isAssetURL } from "../../helpers/utils";
import { test as baseTest, Browser, chromium, expect } from "../../test";

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

    // Using our sitemap.xml, we'll create a pathnames array
    // We cannot use our POM, we must
    // create context manually with browser.newContext()
    test.beforeAll(async ({ browser }) => {
        const page = await browser.newPage();
        const response = await page.goto("/sitemap.xml");
        const sitemapXml = await response!.text();
        // Since we don't want to use any external XML parsing libraries,
        // we can use page.evaluate, but that creates its own execution context
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
        expect(urlPaths.length).toBeGreaterThan(0); // Ensure that paths were fetched correctly
    });

    test("Check all public-facing URLs and their links for a valid 200 response", async ({
        page,
        frontendWarningsLogPath,
        isFrontendWarningsLog,
        baseURL,
    }) => {
        let aggregateHref = [];
        // Set test timeout to be 1 minute instead of 30 seconds
        test.setTimeout(120000);
        for (const path of urlPaths) {
            await page.goto(path, {
                waitUntil: "networkidle",
            });

            const allATags = await page.getByRole("link", { includeHidden: true }).elementHandles();

            for (const aTag of allATags) {
                const href = await aTag.getAttribute("href");
                // ONLY include http, https and relative path names
                if (href && /^(https?:|\/)/.test(href)) {
                    aggregateHref.push(href);
                }
            }
        }

        // Remove duplicate links
        aggregateHref = [...new Set(aggregateHref)];

        const axiosInstance = axios.create({
            timeout: 10000,
        });

        const warnings: { url: string; message: string }[] = [];

        const validateLink = async (browser: Browser, url: string) => {
            // Our app does not properly handle 200 vs 400 HTTP codes for our pages
            // so we cannot simply use Axios since it's an HTTP client only.
            // This means we must actually navigate to the page(s) with Playwright
            // to then decipher the rendered HTML DOM content to then determine
            // if the page is valid or not. isAbsoluteURL determines if the page
            // is an internal link or external one by determining if it's an
            // absolute URL or a relative URL.

            if (isAbsoluteURL(url) || isAssetURL(url)) {
                try {
                    const normalizedURL = new URL(url, baseURL).toString();
                    const response = await axiosInstance.get(normalizedURL);
                    return { url, status: response.status };
                } catch (error) {
                    const e = error as AxiosError;
                    warnings.push({ url, message: e.message });
                    return { url, status: e.response ? e.response.status : 400 };
                }
            } else {
                // For internal relative URLs, use Playwright to navigate and check the page content
                const context = await browser.newContext();
                const page = await context.newPage();

                try {
                    const absoluteUrl = new URL(url, baseURL).toString();
                    await page.goto(absoluteUrl, { waitUntil: "load" });

                    const pageContent = await page.content();
                    const hasPageNotFoundText = pageContent.includes(pageNotFound);
                    const isErrorWrapperVisible = await page.locator('[data-testid="error-page-wrapper"]').isVisible();

                    if (hasPageNotFoundText && isErrorWrapperVisible) {
                        warnings.push({ url, message: "Internal link: Page not found" });
                        return { url, status: 404 };
                    }

                    return { url, status: 200 };
                } catch (_error) {
                    warnings.push({ url, message: "Internal link: Page error" });
                    return { url, status: 400 };
                } finally {
                    await page.close();
                    await context.close();
                }
            }
        };

        const browser = await chromium.launch();

        const results = [];
        const ignorePaths = ["/404", "/400", "/500"];
        const ignoreUrlPaths = Object.keys(["https://reportstream.cdc.gov/onboarding/code-mapping"]).filter(
            (path) => !ignorePaths.includes(path),
        );

        for (const href of aggregateHref) {
            const isReportstream = href.includes("https://reportstream.cdc.gov");
            const isAllowed = ignoreUrlPaths.some((path) => href.includes(path));

            if (isReportstream && !isAllowed) {
                // Skip this link
                console.warn(`Skipping ${href} (reportstream link not in urlPaths)`);
                continue;
            }

            try {
                const result = await validateLink(browser, href);
                results.push(result);
            } catch (error) {
                console.error(`Issue validating link: ${href}`, error);
                results.push({ url: href, status: 500 });
            }
        }

        await browser.close();

        if (isFrontendWarningsLog && warnings.length > 0) {
            fs.writeFileSync(frontendWarningsLogPath, `${JSON.stringify(warnings)}\n`);
        }

        results.forEach((result) => {
            if (result.status !== 200) {
                console.warn(`Warning: ${result.url} returned status ${result.status}`);
            }
        });

        // Required expect statement + if somehow the warnings and number of links
        // are the same, that's a huge problem.
        expect(warnings.length).toBeLessThan(aggregateHref.length);
    });
});
