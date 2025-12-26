/* eslint-disable playwright/no-networkidle */
import axios, { AxiosError } from "axios";
import * as fs from "fs";
import { pageNotFound } from "../../../src/content/error/ErrorMessages";
import { isAbsoluteURL, isAssetURL } from "../../helpers/utils";
import { test as baseTest, chromium, expect, Page } from "../../test";

const test = baseTest.extend({});

// To save bandwidth, this test is within the /spec/chromium-only/ folder
// Since we're just checking link validity. This is specified within our
// Playwright.config here:
// {
//   name: "chromium-only",
//   use: { browserName: "chromium" },
//   testMatch: "spec/chromium-only/*.spec.ts",
// },

async function getUrlPathsFromSitemap(sitemapXml: string, page: Page): Promise<string[]> {
    // Since we don't want to use any external XML parsing libraries,
    // we can use page.evaluate, but that creates its own execution context
    // wherein we need to explicitly return something, which is why
    // we have the convoluted
    // elem.textContent ? new URL(elem.textContent).pathname : null,
    // combined with the .filter for null values
    return await page.evaluate((xmlStr) => {
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(xmlStr, "text/xml");
        const urlElements = xmlDoc.querySelectorAll("urlset url loc");
        return Array.from(urlElements)
            .map((elem) => (elem.textContent ? new URL(elem.textContent).pathname : null))
            .filter((path) => path !== null);
    }, sitemapXml);
}

async function getLinksFromUrlPaths(urlPaths: string[], page: Page): Promise<string[]> {
    // Collect all links from all public pages just once
    const linksSet = new Set<string>();
    for (const path of urlPaths) {
        await page.goto(path, { waitUntil: "networkidle" });
        const allATags = await page.getByRole("link", { includeHidden: true }).elementHandles();
        for (const aTag of allATags) {
            const href = await aTag.getAttribute("href");
            if (href && /^(https?:|\/)/.test(href)) {
                linksSet.add(href);
            }
        }
    }
    return [...linksSet];
}

test.describe("Evaluate links on public facing pages", { tag: "@warning" }, () => {
    let urlPathsFromSitemap: string[] = [];
    let linksFromUrlPaths: string[] = [];

    // Using our sitemap.xml, we'll create a pathnames array
    // We cannot use our POM, we must
    // create context manually with browser.newContext()
    test.beforeAll(async ({ browser }) => {
        const page = await browser.newPage();
        const response = await page.goto("/sitemap.xml");
        const sitemapXml = await response!.text();
        urlPathsFromSitemap = await getUrlPathsFromSitemap(sitemapXml, page);
        linksFromUrlPaths = await getLinksFromUrlPaths(urlPathsFromSitemap, page);
        await page.close();
    });

    test("Check if paths were fetched", () => {
        expect(urlPathsFromSitemap.length).toBeGreaterThan(0); // Ensure that paths were fetched correctly
    });

    test("Check all public-facing links for a valid response", async ({
        baseURL,
        isFrontendWarningsLog,
        frontendWarningsLogPath,
    }) => {
        const axiosInstance = axios.create({ timeout: 10000 });
        const externalLinks = linksFromUrlPaths.filter((link) => isAbsoluteURL(link) || isAssetURL(link));
        const internalLinks = linksFromUrlPaths.filter((link) => !isAbsoluteURL(link) && !isAssetURL(link));
        const allLinkErrors: { url: string; message: string }[] = [];

        // --- External link validation ---
        await Promise.all(
            externalLinks.map(async (url) => {
                try {
                    const normalizedURL = new URL(url, baseURL).toString();
                    const response = await axiosInstance.get(normalizedURL);
                    if (response.status !== 200) {
                        allLinkErrors.push({ url, message: `External link: Status ${response.status}` });
                    }
                } catch (error) {
                    const e = error as AxiosError;
                    allLinkErrors.push({ url, message: e.message });
                }
            }),
        );

        // --- Internal link validation ---
        const browser = await chromium.launch();

        await Promise.all(
            internalLinks.map(async (url) => {
                let isReportstream = false;
                try {
                    const parsedUrl = new URL(url, baseURL);
                    isReportstream = parsedUrl.host === "reportstream.cdc.gov";
                } catch (error) {
                    console.warn(`Invalid URL encountered: ${url}`, error);
                }

                // Normalize each href to just its pathname (like /onboarding/code-mapping)
                // then compare it directly to entries in urlPaths
                let hrefPath = "";
                try {
                    const parsed = new URL(url, baseURL);
                    hrefPath = parsed.pathname;
                } catch (_) {
                    // fallback for relative URLs
                    hrefPath = url;
                }
                console.warn(`Checking internal link: ${url} (normalized path: ${hrefPath})`);
                const isAllowed = urlPathsFromSitemap.includes(hrefPath);

                if (isReportstream && !isAllowed) {
                    // Skip this link
                    console.warn(`Skipping ${url} (reportstream link not in urlPaths)`);
                    return;
                }

                const context = await browser.newContext();
                const page = await context.newPage();
                try {
                    const absoluteUrl = new URL(url, baseURL).toString();
                    const homeUrl = new URL("/", baseURL).toString();
                    await page.goto(homeUrl, { waitUntil: "networkidle" });
                    await page.evaluate((path) => {
                        window.history.pushState({}, "", path);
                        window.dispatchEvent(new PopStateEvent("popstate"));
                    }, url);

                    const pageContent = await page.content();
                    const hasPageNotFoundText = pageContent.includes(pageNotFound);
                    const isErrorWrapperVisible = await page.locator('[data-testid="error-page-wrapper"]').isVisible();

                    if (hasPageNotFoundText && isErrorWrapperVisible) {
                        allLinkErrors.push({ url, message: `Internal link: Page not found ${absoluteUrl}` });
                    }
                } catch (_error) {
                    allLinkErrors.push({ url, message: "Internal link: Page error" });
                } finally {
                    await page.close();
                    await context.close();
                }
            }),
        );
        await browser.close();

        // --- Logging and assertions ---
        if (isFrontendWarningsLog && allLinkErrors.length > 0) {
            fs.writeFileSync(frontendWarningsLogPath, JSON.stringify(allLinkErrors, null, 2) + "\n");
        }

        allLinkErrors.forEach((err) => {
            console.warn(`Warning: ${err.url} - ${err.message}`);
        });

        console.warn(`Total link warnings: ${allLinkErrors.length} out of ${linksFromUrlPaths.length} links checked.`);

        // Required expect statement + if somehow the warnings and number of links
        // are the same, that's a huge problem.
        expect(allLinkErrors.length).toBeLessThan(linksFromUrlPaths.length);
    });
});
