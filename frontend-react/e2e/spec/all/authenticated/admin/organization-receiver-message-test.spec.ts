import { expect } from "@playwright/test";
import {
    errorMessageResult,
    passMessageResult,
    warningMessageResult,
} from "../../../../../src/components/Admin/MessageTesting/MessageTestingResult.fixtures";
import type { RSMessageResult } from "../../../../../src/config/endpoints/reports";
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
        test("redirects to login", async ({ organizationReceiverMessageTestPage }) => {
            await expect(organizationReceiverMessageTestPage.page).toHaveURL("/login");
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });
        test("returns Page Not Found", async ({ organizationReceiverMessageTestPage }) => {
            await expect(organizationReceiverMessageTestPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });
        test("returns Page Not Found", async ({ organizationReceiverMessageTestPage }) => {
            await expect(organizationReceiverMessageTestPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("header", () => {
            test("has correct title + heading", async ({ organizationReceiverMessageTestPage }) => {
                await organizationReceiverMessageTestPage.testHeader();
            });
        });

        test("if there is an error, the error is shown on the page", async ({
            organizationReceiverMessageTestPage,
        }) => {
            organizationReceiverMessageTestPage.mockError = true;
            await organizationReceiverMessageTestPage.reload();
            await expect(organizationReceiverMessageTestPage.page.getByText("there was an error")).toBeVisible();
        });

        test.describe("when there is no error", () => {
            test("has correct title", async ({ organizationReceiverMessageTestPage }) => {
                await expect(organizationReceiverMessageTestPage.page).toHaveURL(
                    organizationReceiverMessageTestPage.url,
                );
                await expect(organizationReceiverMessageTestPage.page).toHaveTitle(
                    organizationReceiverMessageTestPage.title,
                );
            });

            test("displays test messages", async ({ organizationReceiverMessageTestPage }) => {
                await expect(organizationReceiverMessageTestPage.form).toBeVisible();
                let i = 0;
                for (const message of organizationReceiverMessageTestPage.testMessages) {
                    i++;
                    const option = organizationReceiverMessageTestPage.form.getByLabel(message.fileName);
                    await expect(option).toBeVisible();
                    await expect(option).toHaveValue(message.reportBody);
                }
                expect(organizationReceiverMessageTestPage.testMessages.length).toEqual(i);
            });

            test.describe("custom message", () => {
                test("cannot add empty custom message", async ({ organizationReceiverMessageTestPage }) => {
                    await expect(organizationReceiverMessageTestPage.form).toBeVisible();

                    await organizationReceiverMessageTestPage.addCustomMessageButton.click();
                    await expect(organizationReceiverMessageTestPage.submitCustomMessageButton).toBeDisabled();
                });

                test("can add custom message", async ({ organizationReceiverMessageTestPage }) => {
                    const message1 = '{"foo":"bar"}';
                    const message2 = '{"bar":"foo"}';

                    await expect(organizationReceiverMessageTestPage.form).toBeVisible();
                    await expect(organizationReceiverMessageTestPage.addCustomMessageButton).toBeVisible();
                    await expect(organizationReceiverMessageTestPage.submitButton).toBeVisible();

                    for (const [i, message] of [message1, message2].entries()) {
                        const filename = `Custom message ${i + 1}`;
                        const customOption = organizationReceiverMessageTestPage.form.getByLabel(filename);
                        const customOptionViewButton = organizationReceiverMessageTestPage.form
                            .getByText(filename)
                            .getByRole("button");

                        await organizationReceiverMessageTestPage.addCustomMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.customMessageTextArea).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submitCustomMessageButton).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.cancelCustomMessageButton).toBeVisible();

                        await organizationReceiverMessageTestPage.customMessageTextArea.fill(message);
                        await expect(organizationReceiverMessageTestPage.customMessageTextArea).toHaveValue(message);
                        await organizationReceiverMessageTestPage.submitCustomMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.customMessageTextArea).toBeHidden();
                        await expect(organizationReceiverMessageTestPage.submitCustomMessageButton).toBeHidden();
                        await expect(organizationReceiverMessageTestPage.cancelCustomMessageButton).toBeHidden();

                        await expect(customOption).toBeVisible();
                        await expect(customOption).toBeChecked();
                        await expect(customOptionViewButton).toBeVisible();
                        const popupP = organizationReceiverMessageTestPage.page.waitForEvent("popup");
                        await customOptionViewButton.click();
                        const popup = await popupP;
                        await expect(popup.getByText(JSON.stringify(JSON.parse(message), undefined, 2))).toBeVisible();
                    }
                });
            });

            test.describe("form submission", () => {
                test.describe("success", () => {
                    test("stored message", async ({ organizationReceiverMessageTestPage }) => {
                        const message = organizationReceiverMessageTestPage.testMessages[0];
                        await expect(organizationReceiverMessageTestPage.form).toBeVisible();

                        const option = organizationReceiverMessageTestPage.form.getByLabel(message.fileName);
                        const optionLabel = organizationReceiverMessageTestPage.form.getByText(message.fileName);
                        const optionValue = await option.inputValue();
                        await expect(option).toBeVisible();
                        await optionLabel.click();
                        await expect(option).toBeChecked();

                        organizationReceiverMessageTestPage.addMockTestSubmissionHandler();
                        const req = await organizationReceiverMessageTestPage.submit();
                        const res = await req.response();
                        const resJson = (await res?.json()) as RSMessageResult;
                        expect(req.postData()).toEqual(optionValue);
                        expect(resJson).toEqual(passMessageResult);

                        await expect(organizationReceiverMessageTestPage.submitStatus).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submitStatus).toHaveText(
                            organizationReceiverMessageTestPage.expectedStatusSuccess,
                        );
                        await expect(organizationReceiverMessageTestPage.submissionOutputMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionOutputMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionOutputMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionOutputMessage).toHaveText(
                            resJson.message ?? "",
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTestMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTestMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toHaveText(
                            JSON.stringify(JSON.parse(optionValue), undefined, 2),
                        );
                    });

                    test("custom message", async ({ organizationReceiverMessageTestPage }) => {
                        const message = '{"foo":"bar"}';
                        const [option, optionLabel] =
                            await organizationReceiverMessageTestPage.addCustomMessage(message);
                        await expect(organizationReceiverMessageTestPage.form).toBeVisible();

                        const optionValue = await option.inputValue();
                        await expect(option).toBeVisible();
                        await optionLabel.click();
                        await expect(option).toBeChecked();

                        organizationReceiverMessageTestPage.addMockTestSubmissionHandler();
                        const req = await organizationReceiverMessageTestPage.submit();
                        const res = await req.response();
                        const resJson = (await res?.json()) as RSMessageResult;
                        expect(req.postData()).toEqual(optionValue);
                        expect(resJson).toEqual(passMessageResult);

                        await expect(organizationReceiverMessageTestPage.submitStatus).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submitStatus).toHaveText(
                            organizationReceiverMessageTestPage.expectedStatusSuccess,
                        );
                        await expect(organizationReceiverMessageTestPage.submissionOutputMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionOutputMessageButton.click();

                        await expect(organizationReceiverMessageTestPage.submissionOutputMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionOutputMessage).toHaveText(
                            resJson.message ?? "",
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTestMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTestMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toHaveText(
                            JSON.stringify(JSON.parse(message), undefined, 2),
                        );
                    });
                });

                test.describe("warning", () => {
                    test("stored message", async ({ organizationReceiverMessageTestPage }) => {
                        const message = organizationReceiverMessageTestPage.testMessages[0];
                        await expect(organizationReceiverMessageTestPage.form).toBeVisible();

                        const option = organizationReceiverMessageTestPage.form.getByLabel(message.fileName);
                        const optionLabel = organizationReceiverMessageTestPage.form.getByText(message.fileName);
                        const optionValue = await option.inputValue();
                        await expect(option).toBeVisible();
                        await optionLabel.click();
                        await expect(option).toBeChecked();

                        organizationReceiverMessageTestPage.addMockTestSubmissionHandler("warn");
                        const req = await organizationReceiverMessageTestPage.submit();
                        const res = await req.response();
                        const resJson = (await res?.json()) as RSMessageResult;
                        expect(req.postData()).toEqual(optionValue);
                        expect(resJson).toEqual(warningMessageResult);

                        await expect(organizationReceiverMessageTestPage.submitStatus).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submitStatus).toHaveText(
                            organizationReceiverMessageTestPage.expectedStatusWarning,
                        );
                        await expect(
                            organizationReceiverMessageTestPage.submissionTransformWarningsButton,
                        ).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTransformWarningsButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTransformWarnings).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTransformWarnings).toHaveText(
                            [
                                ...resJson.senderTransformWarnings,
                                ...resJson.enrichmentSchemaWarnings,
                                ...resJson.receiverTransformWarnings,
                            ].join(""),
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTestMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTestMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toHaveText(
                            JSON.stringify(JSON.parse(message.reportBody), undefined, 2),
                        );
                        const fileStats = await organizationReceiverMessageTestPage.downloadPDF();
                        expect(fileStats.size).toBeGreaterThan(0);
                    });

                    test("custom message", async ({ organizationReceiverMessageTestPage }) => {
                        const message = '{"foo":"bar"}';
                        const [option, optionLabel] =
                            await organizationReceiverMessageTestPage.addCustomMessage(message);
                        await expect(organizationReceiverMessageTestPage.form).toBeVisible();

                        const optionValue = await option.inputValue();
                        await expect(option).toBeVisible();
                        await optionLabel.click();
                        await expect(option).toBeChecked();

                        organizationReceiverMessageTestPage.addMockTestSubmissionHandler("warn");
                        const req = await organizationReceiverMessageTestPage.submit();
                        const res = await req.response();
                        const resJson = (await res?.json()) as RSMessageResult;
                        expect(req.postData()).toEqual(optionValue);
                        expect(resJson).toEqual(warningMessageResult);

                        await expect(organizationReceiverMessageTestPage.submitStatus).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submitStatus).toHaveText(
                            organizationReceiverMessageTestPage.expectedStatusWarning,
                        );
                        await expect(
                            organizationReceiverMessageTestPage.submissionTransformWarningsButton,
                        ).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTransformWarningsButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTransformWarnings).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTransformWarnings).toHaveText(
                            [
                                ...resJson.senderTransformWarnings,
                                ...resJson.enrichmentSchemaWarnings,
                                ...resJson.receiverTransformWarnings,
                            ].join(""),
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTestMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTestMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toHaveText(
                            JSON.stringify(JSON.parse(message), undefined, 2),
                        );
                        const fileStats = await organizationReceiverMessageTestPage.downloadPDF();
                        expect(fileStats.size).toBeGreaterThan(0);
                    });
                });

                test.describe("failure", () => {
                    test("stored message", async ({ organizationReceiverMessageTestPage }) => {
                        const message = organizationReceiverMessageTestPage.testMessages[0];
                        await expect(organizationReceiverMessageTestPage.form).toBeVisible();

                        const option = organizationReceiverMessageTestPage.form.getByLabel(message.fileName);
                        const optionLabel = organizationReceiverMessageTestPage.form.getByText(message.fileName);
                        const optionValue = await option.inputValue();
                        await expect(option).toBeVisible();
                        await optionLabel.click();
                        await expect(option).toBeChecked();

                        organizationReceiverMessageTestPage.addMockTestSubmissionHandler("fail");
                        const req = await organizationReceiverMessageTestPage.submit();
                        const res = await req.response();
                        const resJson = (await res?.json()) as RSMessageResult;
                        expect(req.postData()).toEqual(optionValue);
                        expect(resJson).toEqual(errorMessageResult);

                        await expect(organizationReceiverMessageTestPage.submitStatus).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submitStatus).toHaveText(
                            organizationReceiverMessageTestPage.expectedStatusFailure,
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTransformErrorsButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTransformErrorsButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTransformErrors).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTransformErrors).toHaveText(
                            [
                                ...resJson.senderTransformErrors,
                                ...resJson.enrichmentSchemaErrors,
                                ...resJson.receiverTransformErrors,
                            ].join(""),
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTestMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTestMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toHaveText(
                            JSON.stringify(JSON.parse(message.reportBody), undefined, 2),
                        );
                        const fileStats = await organizationReceiverMessageTestPage.downloadPDF();
                        expect(fileStats.size).toBeGreaterThan(0);
                    });

                    test("custom message", async ({ organizationReceiverMessageTestPage }) => {
                        const message = '{"foo":"bar"}';
                        const [option, optionLabel] =
                            await organizationReceiverMessageTestPage.addCustomMessage(message);
                        await expect(organizationReceiverMessageTestPage.form).toBeVisible();

                        const optionValue = await option.inputValue();
                        await expect(option).toBeVisible();
                        await optionLabel.click();
                        await expect(option).toBeChecked();

                        organizationReceiverMessageTestPage.addMockTestSubmissionHandler("fail");
                        const req = await organizationReceiverMessageTestPage.submit();
                        const res = await req.response();
                        const resJson = (await res?.json()) as RSMessageResult;
                        expect(req.postData()).toEqual(optionValue);
                        expect(resJson).toEqual(errorMessageResult);

                        await expect(organizationReceiverMessageTestPage.submitStatus).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submitStatus).toHaveText(
                            organizationReceiverMessageTestPage.expectedStatusFailure,
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTransformErrorsButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTransformErrorsButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTransformErrors).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTransformErrors).toHaveText(
                            [
                                ...resJson.senderTransformErrors,
                                ...resJson.enrichmentSchemaErrors,
                                ...resJson.receiverTransformErrors,
                            ].join(""),
                        );
                        await expect(organizationReceiverMessageTestPage.submissionTestMessageButton).toBeVisible();
                        await organizationReceiverMessageTestPage.submissionTestMessageButton.click();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toBeVisible();
                        await expect(organizationReceiverMessageTestPage.submissionTestMessage).toHaveText(
                            JSON.stringify(JSON.parse(message), undefined, 2),
                        );
                        const fileStats = await organizationReceiverMessageTestPage.downloadPDF();
                        expect(fileStats.size).toBeGreaterThan(0);
                    });
                });
            });
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
