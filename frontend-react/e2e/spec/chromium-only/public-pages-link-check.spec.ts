/* eslint-disable playwright/no-networkidle */
import axios, { AxiosError } from "axios";
import * as fs from "fs";
import { pageNotFound } from "../../../src/content/error/ErrorMessages";
import { isAbsoluteURL, isAssetURL } from "../../helpers/utils";
import { test as baseTest, Browser, chromium, expect, Page } from "../../test";

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
    let urlPaths: string[] = [];
    let links: string[] = [];

    // Using our sitemap.xml, we'll create a pathnames array
    // We cannot use our POM, we must
    // create context manually with browser.newContext()
    test.beforeAll(async ({ browser }) => {
        const page = await browser.newPage();
        const response = await page.goto("/sitemap.xml");
        const sitemapXml = await response!.text();
        urlPaths = await getUrlPathsFromSitemap(sitemapXml, page);
        links = await getLinksFromUrlPaths(urlPaths, page);
        await page.close();
    });

    test("Check if paths were fetched", () => {
        expect(urlPaths.length).toBeGreaterThan(0); // Ensure that paths were fetched correctly
    });

    test("Check external links on public-facing URLs for a valid 200 response", async ({
        frontendWarningsLogPath,
        isFrontendWarningsLog,
        baseURL,
    }) => {
        const axiosInstance = axios.create({
            timeout: 10000,
        });

        const externalLinks = links.filter((link) => isAbsoluteURL(link) || isAssetURL(link));
        const linkErrors: { url: string; message: string }[] = [];

        async function validateExternalLink(url: string) {
            try {
                const normalizedURL = new URL(url, baseURL).toString();
                const response = await axiosInstance.get(normalizedURL);
                return { url, status: response.status };
            } catch (error) {
                const e = error as AxiosError;
                linkErrors.push({ url, message: e.message });
                return { url, status: e.response ? e.response.status : 400 };
            }
        }

        const results = await Promise.all(externalLinks.map((externalLink) => validateExternalLink(externalLink)));

        if (isFrontendWarningsLog && linkErrors.length > 0) {
            fs.writeFileSync(frontendWarningsLogPath, `${JSON.stringify(linkErrors)}\n`);
        }

        results.forEach((result) => {
            if (result.status !== 200) {
                console.warn(`Warning: ${result.url} returned status ${result.status}`);
            }
        });
        console.warn(
            `Total external link with errors: ${linkErrors.length} out of ${externalLinks.length} links checked.`,
        );

        // Required expect statement + if somehow the warnings and number of links
        // are the same, that's a huge problem.
        expect(linkErrors.length).toBeLessThan(externalLinks.length);
    });

    test("Check internal links on public-facing URLs for a valid response", async ({
        baseURL,
        isFrontendWarningsLog,
        frontendWarningsLogPath,
    }) => {
        const internalLinks = links.filter((link) => !isAbsoluteURL(link) && !isAssetURL(link));
        const linkErrors: { url: string; message: string }[] = [];

        async function validateInternalLink(url: string, browser: Browser) {
            // For internal relative URLs, use Playwright to navigate and check the page content
            const context = await browser.newContext();
            const page = await context.newPage();

            try {
                const absoluteUrl = new URL(url, baseURL).toString();

                // Go to home first to initialize the SPA
                const homeUrl = new URL("/", baseURL).toString();
                await page.goto(homeUrl, { waitUntil: "networkidle" });

                // Use client-side navigation
                await page.evaluate((path) => {
                    window.history.pushState({}, "", path);
                    window.dispatchEvent(new PopStateEvent("popstate"));
                }, url);

                const pageContent = await page.content();
                const hasPageNotFoundText = pageContent.includes(pageNotFound);
                const isErrorWrapperVisible = await page.locator('[data-testid="error-page-wrapper"]').isVisible();

                if (hasPageNotFoundText && isErrorWrapperVisible) {
                    linkErrors.push({ url, message: `Internal link: Page not found ${absoluteUrl}` });
                    return { url, status: 404 };
                }

                return { url, status: 200 };
            } catch (_error) {
                linkErrors.push({ url, message: "Internal link: Page error" });
                return { url, status: 400 };
            } finally {
                await page.close();
                await context.close();
            }
        }

        const browser = await chromium.launch();

        const results: { url: string; status: number }[] = [];

        await Promise.all(
            internalLinks.map(async (internalLink: string) => {
                let isReportstream = false;
                try {
                    const parsedUrl = new URL(internalLink, baseURL);
                    isReportstream = parsedUrl.host === "reportstream.cdc.gov";
                } catch (error) {
                    console.warn(`Invalid URL encountered: ${internalLink}`, error);
                }

                // Normalize each href to just its pathname (like /onboarding/code-mapping)
                // then compare it directly to entries in urlPaths
                let hrefPath = "";
                try {
                    const parsed = new URL(internalLink, baseURL);
                    hrefPath = parsed.pathname;
                } catch (_) {
                    // fallback for relative URLs
                    hrefPath = internalLink;
                }

                const isAllowed = urlPaths.includes(hrefPath);

                if (isReportstream && !isAllowed) {
                    // Skip this link
                    console.warn(`Skipping ${internalLink} (reportstream link not in urlPaths)`);
                    return;
                }

                try {
                    const result = await validateInternalLink(internalLink, browser);
                    results.push(result);
                } catch (error) {
                    console.error(`Issue validating link: ${internalLink}`, error);
                    results.push({ url: internalLink, status: 500 });
                }
            }),
        );

        await browser.close();

        if (isFrontendWarningsLog && linkErrors.length > 0) {
            fs.writeFileSync(frontendWarningsLogPath, `${JSON.stringify(linkErrors)}\n`);
        }

        results.forEach((result) => {
            if (result.status !== 200) {
                console.warn(`Warning status: ${result.url} returned status ${result.status}`);
            }
        });

        // Required expect statement + if somehow the warnings and number of links
        // are the same, that's a huge problem.
        console.warn(`Total warnings: ${linkErrors.length} out of ${internalLinks.length} links checked.`);
        expect(linkErrors.length).toBeLessThan(internalLinks.length);
    });
});
