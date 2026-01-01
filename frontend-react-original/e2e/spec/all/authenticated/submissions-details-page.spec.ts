import { expect } from "@playwright/test";
import * as submissionDetails from "../../../pages/authenticated/submission-history";
import { URL_SUBMISSION_HISTORY } from "../../../pages/authenticated/submission-history";
import { id, SubmissionsDetailsPage } from "../../../pages/authenticated/submissions-details";
import { test as baseTest, logins } from "../../../test";

export interface SubmissionsDetailsPageFixtures {
    submissionsDetailsPage: SubmissionsDetailsPage;
}

const test = baseTest.extend<SubmissionsDetailsPageFixtures>({
    submissionsDetailsPage: async (
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
        const page = new SubmissionsDetailsPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
            isTestOrg: true,
        });
        await page.goto();
        await use(page);
    },
});

test.describe("Submissions Details page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ submissionsDetailsPage }) => {
            await expect(submissionsDetailsPage.page).toHaveURL("/login");
        });
    });

    test.describe("admin user - happy path", () => {
        test.use({ storageState: logins.admin.path });

        test.describe("without org selected", () => {
            test("has correct title", async ({ submissionsDetailsPage }) => {
                await expect(submissionsDetailsPage.page).toHaveTitle(submissionsDetailsPage.title);
            });

            test("has reportId in breadcrumb", async ({ submissionsDetailsPage }) => {
                await expect(submissionsDetailsPage.page.locator(".usa-breadcrumb ol li").nth(1)).toHaveText(
                    `Details: ${id}`,
                );
            });

            test("has footer", async ({ submissionsDetailsPage }) => {
                await expect(submissionsDetailsPage.page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test("breadcrumb navigates to Submission History page", async ({ submissionsDetailsPage }) => {
                await submissionDetails.breadcrumbLink(
                    submissionsDetailsPage.page,
                    0,
                    "Submissions",
                    URL_SUBMISSION_HISTORY,
                );
            });

            test("has footer", async ({ submissionsDetailsPage }) => {
                await expect(submissionsDetailsPage.page.locator("footer")).toBeAttached();
            });
        });
    });

    test.describe("admin user - server error", () => {
        test.use({ storageState: logins.admin.path });

        test("error is shown on the page", async ({ submissionsDetailsPage }) => {
            submissionsDetailsPage.mockError = true;
            await submissionsDetailsPage.reload();

            await expect(submissionsDetailsPage.page.getByText(/An error has occurred./)).toBeAttached();
            await expect(submissionsDetailsPage.page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user - happy path", () => {
        test.use({ storageState: logins.sender.path });

        test("has correct title", async ({ submissionsDetailsPage }) => {
            await expect(submissionsDetailsPage.page).toHaveTitle(submissionsDetailsPage.title);
        });

        test("has reportId in breadcrumb", async ({ submissionsDetailsPage }) => {
            await expect(submissionsDetailsPage.page.locator(".usa-breadcrumb ol li").nth(1)).toHaveText(
                `Details: ${id}`,
            );
        });

        test("breadcrumb navigates to Submission History page", async ({ submissionsDetailsPage }) => {
            await submissionDetails.breadcrumbLink(
                submissionsDetailsPage.page,
                0,
                "Submissions",
                URL_SUBMISSION_HISTORY,
            );
        });

        test("has footer", async ({ submissionsDetailsPage }) => {
            await expect(submissionsDetailsPage.page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user - server error", () => {
        test.use({ storageState: logins.sender.path });

        test("has error message", async ({ submissionsDetailsPage }) => {
            submissionsDetailsPage.mockError = true;
            await submissionsDetailsPage.reload();
            await expect(submissionsDetailsPage.page.getByText(/An error has occurred./)).toBeAttached();
            await expect(submissionsDetailsPage.page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: logins.receiver.path });

        test("has error message", async ({ submissionsDetailsPage }) => {
            submissionsDetailsPage.mockError = true;
            await submissionsDetailsPage.reload();
            await expect(submissionsDetailsPage.page.getByText(/An error has occurred./)).toBeAttached();
            await expect(submissionsDetailsPage.page.locator("footer")).toBeAttached();
        });
    });
});
