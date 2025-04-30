import { expect, test } from "@playwright/test";

test.describe("Code Mapping Tool Page", () => {
    test("not authenticated user", async ({ page }) => {
        await page.goto("/onboarding/code-mapping");
        await expect(page).toHaveURL("/login");
    });

    /*
     *
     * Admin User
     *
     * */
    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test("page loads", async ({ page }) => {
            await page.goto("/onboarding/code-mapping");
            const pageHeader = page.locator("h1");
            await expect(pageHeader).toHaveText("Code mapping tool");
            await expect(page.locator("footer")).toBeAttached();
        });

        // if the admin does not have a sender profile set
        test("uploads a CSV file and should error", async ({ page }) => {
            await page.goto("/onboarding/code-mapping");
            const fileInput = page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_unmapped_codes.csv", { noWaitAfter: true });
            await page.click('button:has-text("Submit")');

            const alertHeader = page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(alertHeader).toHaveText("Bad Request 400");
            await expect(page.getByText("Bad Request 400")).toBeVisible();
        });
    });

    /*
     *
     * Sender User
     *
     * */
    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test("page loads", async ({ page }) => {
            await page.goto("/onboarding/code-mapping");
            const pageHeader = page.locator("h1");
            await expect(pageHeader).toHaveText("Code mapping tool");
            await expect(page.locator("footer")).toBeAttached();
        });

        test("uploads a CSV file with no errors", async ({ page }) => {
            await page.goto("/onboarding/code-mapping");
            const fileInput = page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_success.csv", { noWaitAfter: true });
            await page.click('button:has-text("Submit")');

            const fileName = page.locator("h2 span > p:last-child");
            const alertHeader = page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(fileName).toHaveText("codemapping_success.csv");
            await expect(alertHeader).toHaveText("All codes are mapped");
            await expect(page.getByText("All codes are mapped")).toBeVisible();

            await page.click('button:has-text("Test another file")');
        });

        test("uploads a CSV file with unmapped codes", async ({ page }) => {
            await page.goto("/onboarding/code-mapping");
            const fileInput = page.locator("input.usa-file-input__input");
            await fileInput.setInputFiles("e2e/fixtures/codemapping_unmapped_codes.csv", { noWaitAfter: true });
            await page.click('button:has-text("Submit")');

            const fileName = page.locator("h2 span > p:last-child");
            const alertHeader = page.locator("[data-testid='alert'] .usa-alert__heading");
            await expect(fileName).toHaveText("codemapping_unmapped_codes.csv");
            await expect(alertHeader).toHaveText("Your file contains unmapped codes");
            await expect(page.getByText("Your file contains unmapped codes")).toBeVisible();
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

        test("user", async ({ page }) => {
            await page.goto("/onboarding/code-mapping");
            const errorText = page.locator("p.usa-alert__text");
            await expect(errorText).toHaveText("Our apologies, there was an error loading this content.");
        });
    });

    // END
});
