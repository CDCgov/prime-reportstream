import { expect, test } from "@playwright/test";
import { selectTestOrg } from "../../helpers/utils";
import * as reportDetails from "../../pages/report-details";
import * as submissionDetails from "../../pages/submission-history";
import { URL_SUBMISSION_HISTORY } from "../../pages/submission-history";

const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";
test.describe("Submissions Details page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await submissionDetails.gotoDetails(page, id);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("admin user - happy path", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await reportDetails.mockGetSubmissionHistoryResponse(page, id);
                await submissionDetails.gotoDetails(page, id);
            });

            test("has correct title", async ({ page }) => {
                await submissionDetails.title(page);
            });

            test("has reportId in breadcrumb", async ({ page }) => {
                await expect(
                    page.locator(".usa-breadcrumb ol li").nth(1),
                ).toHaveText(`Details: ${id}`);
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test.beforeEach(async ({ page }) => {
                await selectTestOrg(page);
                await reportDetails.mockGetSubmissionHistoryResponse(page, id);
                await submissionDetails.gotoDetails(page, id);
            });

            test("has correct title", async ({ page }) => {
                await submissionDetails.title(page);
            });

            test("breadcrumb navigates to Submission History page", async ({
                page,
            }) => {
                await submissionDetails.breadcrumbLink(
                    page,
                    0,
                    "Submissions",
                    URL_SUBMISSION_HISTORY,
                );
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });
    });

    test.describe("admin user - server error", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetSubmissionHistoryResponse(page, id, 500);
            await submissionDetails.gotoDetails(page, id);
        });

        test("has error message", async ({ page }) => {
            await expect(
                page.getByText(/An error has occurred./),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user - happy path", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetSubmissionHistoryResponse(page, id);
            await submissionDetails.gotoDetails(page, id);
        });

        test("has correct title", async ({ page }) => {
            await submissionDetails.title(page);
        });

        test("has reportId in breadcrumb", async ({ page }) => {
            await expect(
                page.locator(".usa-breadcrumb ol li").nth(1),
            ).toHaveText(`Details: ${id}`);
        });

        test("breadcrumb navigates to Submission History page", async ({
            page,
        }) => {
            await submissionDetails.breadcrumbLink(
                page,
                0,
                "Submissions",
                URL_SUBMISSION_HISTORY,
            );
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user - server error", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetSubmissionHistoryResponse(page, id, 500);
            await submissionDetails.gotoDetails(page, id);
        });

        test("has error message", async ({ page }) => {
            await expect(
                page.getByText(/An error has occurred./),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await submissionDetails.gotoDetails(page, id);
        });

        test("has error message", async ({ page }) => {
            await expect(
                page.getByText(/An error has occurred./),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });
});
