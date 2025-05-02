import { CodeMappingPage } from "../../../pages/authenticated/code-mapping";
import { test as baseTest, expect } from "../../../test";

export interface CodeMappingPageFixtures {
    codeMappingPage: CodeMappingPage;
}

const test = baseTest.extend<CodeMappingPageFixtures>({
    codeMappingPage: async (
        {
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        },
        use,
    ) => {
        const page = new CodeMappingPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        });
        await page.goto();
        await use(page);
    },
});

test.describe("Code Mapping Tool Page", () => {
    test("not authenticated user", async ({ codeMappingPage }) => {
        await codeMappingPage.page.goto(CodeMappingPage.URL_CODE_MAPPING);
        await expect(codeMappingPage.page).toHaveURL("/login");
    });

    /*
     *
     * Admin User
     *
     * */
    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test("page loads", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            await codeMappingPage.testHeader();
            await expect(codeMappingPage.page.locator("footer")).toBeAttached();
        });

        // if the admin does not have a sender profile set
        test("uploads a CSV file and should error if sender profile not set", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            const fileInput = codeMappingPage.page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_success.csv", { noWaitAfter: true });
            await codeMappingPage.page.click('button:has-text("Submit")');

            const alertHeader = codeMappingPage.page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(alertHeader).toHaveText("Bad Request 400");
            await expect(codeMappingPage.page.getByText("Bad Request 400")).toBeVisible();
            await expect(codeMappingPage.page.getByText("Expected a 'client' request header")).toBeVisible();
        });

        test("uploads a CSV file with unmapped codes should error", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            const fileInput = codeMappingPage.page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_unmapped_codes.csv", { noWaitAfter: true });
            await codeMappingPage.page.click('button:has-text("Submit")');

            const alertHeader = codeMappingPage.page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(alertHeader).toHaveText("Bad Request 400");
            await expect(codeMappingPage.page.getByText("Bad Request 400")).toBeVisible();
            await expect(codeMappingPage.page.getByText("Expected a 'client' request header")).toBeVisible();
        });

        test("submit button pressed without a file should error", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            await codeMappingPage.page.click('button:has-text("Submit")');

            const alertHeader = codeMappingPage.page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(alertHeader).toHaveText("Bad Request 400");
            await expect(codeMappingPage.page.getByText("Bad Request 400")).toBeVisible();
        });
    });

    /*
     *
     * Sender User
     *
     * */
    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test("page loads", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            await codeMappingPage.testHeader();
            await expect(codeMappingPage.page.locator("footer")).toBeAttached();
        });

        test("uploads a CSV file with no errors", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            const fileInput = codeMappingPage.page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_success.csv", { noWaitAfter: true });
            await codeMappingPage.page.click('button:has-text("Submit")');

            const fileName = codeMappingPage.page.locator("h2 span > p:last-child");
            const alertHeader = codeMappingPage.page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(fileName).toHaveText("codemapping_success.csv");
            await expect(alertHeader).toHaveText("All codes are mapped");
            await expect(codeMappingPage.page.getByText("All codes are mapped")).toBeVisible();

            await codeMappingPage.page.click('button:has-text("Test another file")');
        });

        test("uploads a CSV file with unmapped codes should error", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            const fileInput = codeMappingPage.page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_unmapped_codes.csv", { noWaitAfter: true });
            await codeMappingPage.page.click('button:has-text("Submit")');

            const fileName = codeMappingPage.page.locator("h2 span > p:last-child");
            const alertHeader = codeMappingPage.page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(fileName).toHaveText("codemapping_unmapped_codes.csv");
            await expect(alertHeader).toHaveText("Your file contains unmapped codes");
            await expect(codeMappingPage.page.getByText("Your file contains unmapped codes")).toBeVisible();
        });

        test("submit button pressed without a file should error", async ({ codeMappingPage }) => {
            await codeMappingPage.page.goto("/onboarding/code-mapping");
            await codeMappingPage.page.click('button:has-text("Submit")');

            const alertHeader = codeMappingPage.page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(alertHeader).toHaveText("Bad Request 400");
            await expect(codeMappingPage.page.getByText("Bad Request 400")).toBeVisible();
        });

        test.skip("uploads a CSV file and 'Download table as CSV' button should download the file", async ({
            codeMappingPage,
        }) => {
            const downloadProm = codeMappingPage.page.waitForEvent("download");

            await codeMappingPage.page.goto("/onboarding/code-mapping");
            const fileInput = codeMappingPage.page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_success.csv", { noWaitAfter: true });
            await codeMappingPage.page.click('button:has-text("Submit")');

            const fileName = codeMappingPage.page.locator("h2 span > p:last-child");
            await expect(fileName).toHaveText("codemapping_success.csv");

            await codeMappingPage.page.click('button:has-text("Download table as CSV")');

            const download = await downloadProm;

            // assert filename
            expect(download.suggestedFilename()).toBe(fileName);
        });
    });

    /*
     *
     * Receiver User
     *
     * */
    // This does not work as expected,
    // a receiver user should see an error on page
    test.skip("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test("user", async ({ codeMappingPage }) => {
            const errorText = codeMappingPage.page.locator("p.usa-alert__text");
            await expect(errorText).toHaveText("Our apologies, there was an error loading this content.");
        });
    });

    // END
});
