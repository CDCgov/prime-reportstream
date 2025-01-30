import { expect } from "@playwright/test";
import { pageNotFound } from "../../../../../src/content/error/ErrorMessages";
import { OrganizationReceiverMessageTestPage } from "../../../../pages/authenticated/admin/organization-receiver-message-test";
import { test as baseTest } from "../../../../test";

export interface OrganizationReceiverMessageTestPageFixtures {
    organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage;
}

const test = baseTest.extend<OrganizationReceiverMessageTestPageFixtures>({
    organizationReceiverMessageTestPage: async (
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
        const page = new OrganizationReceiverMessageTestPage({
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

test.describe("Admin Organization Receiver Message Test Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({
            organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
        }) => {
            await expect(OrganizationReceiverMessageTestPage.page).toHaveURL("/login");
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });
        test("returns Page Not Found", async ({
            organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
        }) => {
            await expect(OrganizationReceiverMessageTestPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });
        test("returns Page Not Found", async ({
            organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
        }) => {
            await expect(OrganizationReceiverMessageTestPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("header", () => {
            test("has correct title + heading", async ({
                organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
            }) => {
                await OrganizationReceiverMessageTestPage.testHeader();
            });
        });

        test("if there is an error, the error is shown on the page", async ({
            organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
        }) => {
            OrganizationReceiverMessageTestPage.mockError = true;
            await OrganizationReceiverMessageTestPage.reload();
            await expect(OrganizationReceiverMessageTestPage.page.getByText("there was an error")).toBeVisible();
        });

        test.describe("when there is no error", () => {
            test("has correct title", async ({
                organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
            }) => {
                await expect(OrganizationReceiverMessageTestPage.page).toHaveURL(
                    OrganizationReceiverMessageTestPage.url,
                );
                await expect(OrganizationReceiverMessageTestPage.page).toHaveTitle(
                    OrganizationReceiverMessageTestPage.title,
                );
            });

            test("displays test messages", async ({
                organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
            }) => {
                await expect(OrganizationReceiverMessageTestPage.form).toBeVisible();
                let i = 0;
                for (const message of OrganizationReceiverMessageTestPage.testMessages) {
                    i++;
                    const option = OrganizationReceiverMessageTestPage.form.getByLabel(message.fileName);
                    await expect(option).toBeVisible();
                    await expect(option).toHaveValue(message.reportBody);
                }
                expect(OrganizationReceiverMessageTestPage.testMessages.length).toEqual(i);
            });

            test("can add custom message", async ({
                organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
            }) => {
                let customOption = OrganizationReceiverMessageTestPage.form.getByLabel("Custom message 1");

                await expect(OrganizationReceiverMessageTestPage.form).toBeVisible();
                await expect(OrganizationReceiverMessageTestPage.addCustomMessageButton).toBeVisible();
                await expect(OrganizationReceiverMessageTestPage.submitButton).toBeVisible();

                await OrganizationReceiverMessageTestPage.addCustomMessageButton.click();
                await expect(OrganizationReceiverMessageTestPage.customMessageTextArea).toBeVisible();
                await expect(OrganizationReceiverMessageTestPage.submitCustomMessageButton).toBeVisible();
                await expect(OrganizationReceiverMessageTestPage.cancelCustomMessageButton).toBeVisible();

                await OrganizationReceiverMessageTestPage.customMessageTextArea.fill("test 1");
                await expect(OrganizationReceiverMessageTestPage.customMessageTextArea).toHaveValue("test 1");
                await OrganizationReceiverMessageTestPage.submitCustomMessageButton.click();
                await expect(OrganizationReceiverMessageTestPage.customMessageTextArea).toBeHidden();
                await expect(OrganizationReceiverMessageTestPage.submitCustomMessageButton).toBeHidden();
                await expect(OrganizationReceiverMessageTestPage.cancelCustomMessageButton).toBeHidden();
                await expect(customOption).toBeVisible();
                await expect(customOption).toBeChecked();
                // verify view message button

                customOption = OrganizationReceiverMessageTestPage.form.getByLabel("Custom message 2");
                await OrganizationReceiverMessageTestPage.addCustomMessageButton.click();
                await OrganizationReceiverMessageTestPage.customMessageTextArea.fill("test 2");
                await expect(OrganizationReceiverMessageTestPage.customMessageTextArea).toHaveValue("test 2");
                await OrganizationReceiverMessageTestPage.submitCustomMessageButton.click();
                await expect(OrganizationReceiverMessageTestPage.customMessageTextArea).toBeHidden();
                await expect(OrganizationReceiverMessageTestPage.submitCustomMessageButton).toBeHidden();
                await expect(OrganizationReceiverMessageTestPage.cancelCustomMessageButton).toBeHidden();
                await expect(customOption).toBeVisible();
                await expect(customOption).toBeChecked();
                // verify view message button
            });

            test.describe("form submits", () => {
                test("stored message", async ({
                    organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
                }) => {
                    await expect(OrganizationReceiverMessageTestPage.form).toBeVisible();

                    const firstOption = OrganizationReceiverMessageTestPage.form.getByLabel(
                        OrganizationReceiverMessageTestPage.testMessages[0].fileName,
                    );
                    const firstOptionLabel = OrganizationReceiverMessageTestPage.form.getByText(
                        OrganizationReceiverMessageTestPage.testMessages[0].fileName,
                    );
                    await expect(firstOption).toBeVisible();
                    await firstOptionLabel.click();
                    await expect(firstOption).toBeChecked();

                    // create interceptor
                    // click submit button
                    // check intercepted payload
                });
            });
            // custom message
        });
    });

    test.describe("footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({
            organizationReceiverMessageTestPage: OrganizationReceiverMessageTestPage,
        }) => {
            await OrganizationReceiverMessageTestPage.testFooter();
        });
    });
});
