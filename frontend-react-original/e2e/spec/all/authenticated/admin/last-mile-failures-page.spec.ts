import { pageNotFound } from "../../../../../src/content/error/ErrorMessages";
import { tableRows } from "../../../../helpers/utils";
import { LastMileFailuresPage } from "../../../../pages/authenticated/admin/last-mile-failures";
import { test as baseTest, expect } from "../../../../test";

export interface LastMileFailuresPageFixtures {
    lastMileFailuresPage: LastMileFailuresPage;
}

const test = baseTest.extend<LastMileFailuresPageFixtures>({
    lastMileFailuresPage: async (
        {
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
        },
        use,
    ) => {
        const page = new LastMileFailuresPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
        });
        await page.goto();
        await use(page);
    },
});

test.describe("Last Mile Failure page", () => {
    test.describe("admin user - happy path", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("Header", () => {
            test("has correct title + heading", async ({ lastMileFailuresPage }) => {
                await lastMileFailuresPage.testHeader();
            });
        });

        test("table has correct headers", async ({ lastMileFailuresPage }) => {
            await expect(lastMileFailuresPage.page.locator(".column-header-text").nth(0)).toHaveText(/Failed At/);
            await expect(lastMileFailuresPage.page.locator(".column-header-text").nth(1)).toHaveText(/ReportId/);
            await expect(lastMileFailuresPage.page.locator(".column-header-text").nth(2)).toHaveText(/Receiver/);
        });

        test("table column 'Failed At' has expected data", async ({ lastMileFailuresPage }) => {
            await expect(tableRows(lastMileFailuresPage.page).nth(0).locator("td").nth(0)).toHaveText(
                "Tue, 2/20/2024, 9:35 PM",
            );
        });

        test("table column 'ReportId' will open a modal with report details", async ({ lastMileFailuresPage }) => {
            const reportId = tableRows(lastMileFailuresPage.page).nth(0).locator("td").nth(1);
            await expect(reportId).toContainText(/e5ce49c0-b230-4364-8230-964273249fa1/);
            await reportId.click();

            const modal = lastMileFailuresPage.page.getByTestId("modalWindow").nth(0);
            await expect(modal).toContainText(/Report ID:e5ce49c0-b230-4364-8230-964273249fa1/);
        });

        test("table column 'Receiver' will open receiver edit page", async ({ lastMileFailuresPage }) => {
            const receiver = tableRows(lastMileFailuresPage.page).nth(0).locator("td").nth(2);
            await expect(receiver).toContainText(/flexion.etor-service-receiver-results/);
            await receiver.click();

            await expect(lastMileFailuresPage.page).toHaveURL(
                "/admin/orgreceiversettings/org/flexion/receiver/etor-service-receiver-results/action/edit",
            );
        });
    });

    test.describe("admin user - server error", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test("has alert", async ({ lastMileFailuresPage }) => {
            lastMileFailuresPage.mockError = true;
            await lastMileFailuresPage.reload();

            await expect(lastMileFailuresPage.page.getByTestId("alert")).toBeAttached();
            await expect(
                lastMileFailuresPage.page.getByText(/Our apologies, there was an error loading this content./),
            ).toBeAttached();
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test("returns Page Not Found", async ({ lastMileFailuresPage }) => {
            await expect(lastMileFailuresPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test("returns Page Not Found", async ({ lastMileFailuresPage }) => {
            await expect(lastMileFailuresPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("Footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({ lastMileFailuresPage }) => {
            await lastMileFailuresPage.testFooter();
        });
    });
});
