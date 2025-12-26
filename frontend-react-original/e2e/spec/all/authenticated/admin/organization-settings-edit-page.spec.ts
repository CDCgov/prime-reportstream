import { expect } from "@playwright/test";
import { pageNotFound } from "../../../../../src/content/error/ErrorMessages";
import { tableDataCellValue } from "../../../../helpers/utils";
import { MOCK_GET_ORGANIZATION_IGNORE } from "../../../../mocks/organizations";
import { OrganizationEditPage } from "../../../../pages/authenticated/admin/organization-edit";
import { test as baseTest } from "../../../../test";

export interface OrganizationEditPageFixtures {
    organizationEditPage: OrganizationEditPage;
}

const test = baseTest.extend<OrganizationEditPageFixtures>({
    organizationEditPage: async (
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
        const page = new OrganizationEditPage({
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

test.describe("Organization Edit Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ organizationEditPage }) => {
            await expect(organizationEditPage.page).toHaveURL("/login");
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });
        test("returns Page Not Found", async ({ organizationEditPage }) => {
            await expect(organizationEditPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });
        test("returns Page Not Found", async ({ organizationEditPage }) => {
            await expect(organizationEditPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("header", () => {
            test("has correct title", async ({ organizationEditPage }) => {
                await organizationEditPage.testHeader(false);
            });
        });

        test.describe("when there is an error", () => {
            test("the error is shown on the page", async ({ organizationEditPage }) => {
                organizationEditPage.mockError = true;
                await organizationEditPage.reload();
                await expect(organizationEditPage.page.getByText("there was an error")).toBeVisible();
            });
        });

        test.describe("when there is no error", () => {
            test("has correct title", async ({ organizationEditPage }) => {
                await organizationEditPage.testHeader(false);
            });

            test.describe("edit section", () => {
                test("has expected 'Meta'", async ({ organizationEditPage }) => {
                    const meta = organizationEditPage.page.getByTestId("gridContainer").getByTestId("grid").nth(2);
                    await expect(meta).toHaveText(organizationEditPage.getOrgMeta(MOCK_GET_ORGANIZATION_IGNORE));
                });

                test("has expected 'Description'", async ({ organizationEditPage }) => {
                    await expect(organizationEditPage.page.getByTestId("description")).toHaveValue(
                        MOCK_GET_ORGANIZATION_IGNORE.description,
                    );
                });

                test("has expected 'Jurisdiction'", async ({ organizationEditPage }) => {
                    await expect(organizationEditPage.page.getByTestId("jurisdiction")).toHaveValue(
                        MOCK_GET_ORGANIZATION_IGNORE.jurisdiction,
                    );
                });

                test("has expected 'Filters'", async ({ organizationEditPage }) => {
                    await expect(organizationEditPage.page.getByTestId("filters")).toHaveValue(
                        JSON.stringify(MOCK_GET_ORGANIZATION_IGNORE.filters, null, 2),
                    );
                });
            });

            test.describe("'Preview save...'", () => {
                test.beforeEach(async ({ organizationEditPage }) => {
                    await organizationEditPage.page
                        .getByRole("button", {
                            name: "Preview save...",
                        })
                        .click();
                });

                test("saves and closes modal", async ({ organizationEditPage }) => {
                    const modal = organizationEditPage.page.getByTestId("modalWindow").nth(0);
                    await expect(modal).toContainText(/You are about to change this setting: ignore/);
                });

                test("'Go back' closes modal'", async ({ organizationEditPage }) => {
                    const modal = organizationEditPage.page.getByTestId("modalWindow").nth(0);
                    await expect(modal).toBeVisible();

                    await organizationEditPage.page
                        .getByRole("button", {
                            name: "Go back",
                        })
                        .click();
                    await expect(modal).toBeHidden();
                });
            });

            test.describe("'Organization Sender Settings' section", () => {
                test("can create a new organization sender", async ({ organizationEditPage }) => {
                    await organizationEditPage.page
                        .locator("#orgsendersettings")
                        .getByRole("link", { name: "New" })
                        .click();
                    await expect(organizationEditPage.page).toHaveURL(
                        `/admin/orgnewsetting/org/ignore/settingtype/sender`,
                    );
                    await expect(organizationEditPage.page.getByText(/Org name: ignore/)).toBeVisible();
                    await expect(organizationEditPage.page.getByText(/Setting Type: sender/)).toBeVisible();

                    await organizationEditPage.page.getByTestId("orgSettingName").fill("e2e-test");

                    await organizationEditPage.orgSenderNew.cancel.click();
                });

                test("can edit an organization sender", async ({ organizationEditPage }) => {
                    const firstOrgSender = await organizationEditPage.page
                        .locator("#orgsendersettings")
                        .nth(0)
                        .locator("td")
                        .nth(0)
                        .innerText();
                    await organizationEditPage.page
                        .locator("#orgsendersettings")
                        .getByRole("link", { name: "Edit" })
                        .nth(0)
                        .click();
                    await expect(organizationEditPage.page).toHaveURL(
                        `/admin/orgsendersettings/org/ignore/sender/${firstOrgSender}/action/edit`,
                    );
                    await expect(organizationEditPage.page.getByText(`Org name: ignore`)).toBeVisible();
                    await expect(organizationEditPage.page.getByText(`Sender name: ${firstOrgSender}`)).toBeVisible();

                    await organizationEditPage.orgSenderEdit.editJsonButton.click();
                    const modal = organizationEditPage.page.getByTestId("modalWindow").nth(0);
                    await expect(modal).toBeVisible();

                    // Save button is disabled
                    await expect(organizationEditPage.orgSenderEdit.editJsonModal.save).toHaveAttribute("disabled");

                    await organizationEditPage.orgSenderEdit.editJsonModal.checkSyntax.click();
                    await expect(organizationEditPage.orgSenderEdit.editJsonModal.save).not.toHaveAttribute("disabled");

                    await organizationEditPage.orgSenderEdit.editJsonModal.save.click();
                    await expect(modal).toBeHidden();

                    const orgSenderLocator = firstOrgSender.replace("-", "_");

                    await expect(
                        organizationEditPage.page
                            .locator(`#id_Item__${orgSenderLocator}__has_been_saved`)
                            .getByTestId("alerttoast"),
                    ).toHaveText(`Item '${firstOrgSender}' has been saved`);
                    await expect(organizationEditPage.page).toHaveURL(organizationEditPage.url);
                });

                test("can cancel when editing an organization sender", async ({ organizationEditPage }) => {
                    const firstOrgSender = await tableDataCellValue(organizationEditPage.page, 0, 0);
                    await organizationEditPage.page
                        .locator("#orgsendersettings")
                        .getByRole("link", { name: "Edit" })
                        .nth(0)
                        .click();
                    await expect(organizationEditPage.page).toHaveURL(
                        `/admin/orgsendersettings/org/ignore/sender/${firstOrgSender}/action/edit`,
                    );
                    await expect(organizationEditPage.page.getByText(`Org name: ignore`)).toBeVisible();
                    await expect(organizationEditPage.page.getByText(`Sender name: ${firstOrgSender}`)).toBeVisible();

                    await organizationEditPage.orgSenderEdit.cancelButton.click();
                    await expect(organizationEditPage.page).toHaveURL(organizationEditPage.url);
                });
            });

            test.describe("'Organization Receiver Settings' section", () => {
                test("can create a new organization receiver", async ({ organizationEditPage }) => {
                    await organizationEditPage.page
                        .locator("#orgreceiversettings")
                        .getByRole("link", { name: "New" })
                        .click();
                    await expect(organizationEditPage.page).toHaveURL(
                        `/admin/orgnewsetting/org/ignore/settingtype/receiver`,
                    );
                    await expect(organizationEditPage.page.getByText(/Org name: ignore/)).toBeVisible();
                    await expect(organizationEditPage.page.getByText(/Setting Type: receiver/)).toBeVisible();

                    await organizationEditPage.page.getByTestId("orgSettingName").fill("e2e-test");

                    await organizationEditPage.orgReceiverNew.cancel.click();
                });

                test("can edit an organization receiver", async ({ organizationEditPage }) => {
                    const firstOrgReceiver = await organizationEditPage.page
                        .locator("#orgreceiversettings")
                        .nth(0)
                        .locator("td")
                        .nth(0)
                        .innerText();
                    await organizationEditPage.page
                        .locator("#orgreceiversettings")
                        .getByRole("link", { name: "Edit" })
                        .nth(0)
                        .click();
                    await expect(organizationEditPage.page).toHaveURL(
                        `/admin/orgreceiversettings/org/ignore/receiver/${firstOrgReceiver}/action/edit`,
                    );
                    await expect(organizationEditPage.page.getByText(`Org name: ignore`)).toBeVisible();
                    await expect(
                        organizationEditPage.page.getByText(`Receiver name: ${firstOrgReceiver}`),
                    ).toBeVisible();

                    await organizationEditPage.orgReceiverEdit.editJsonButton.click();
                    const modal = organizationEditPage.page.getByTestId("modalWindow").nth(0);
                    await expect(modal).toBeVisible();

                    // Save button is disabled
                    await expect(organizationEditPage.orgReceiverEdit.editJsonModal.save).toHaveAttribute("disabled");

                    await organizationEditPage.orgReceiverEdit.editJsonModal.checkSyntax.click();
                    await expect(organizationEditPage.orgReceiverEdit.editJsonModal.save).not.toHaveAttribute(
                        "disabled",
                    );

                    await organizationEditPage.orgReceiverEdit.editJsonModal.save.click();
                    await expect(modal).toBeHidden();

                    const orgReceiverLocator = firstOrgReceiver.replace("-", "_");

                    await expect(
                        organizationEditPage.page
                            .locator(`#id_Item__${orgReceiverLocator}__has_been_updated`)
                            .getByTestId("alerttoast"),
                    ).toHaveText(`Item '${firstOrgReceiver}' has been updated`);
                    await expect(organizationEditPage.page).toHaveURL(organizationEditPage.url);
                });

                test("can cancel when editing an organization receiver", async ({ organizationEditPage }) => {
                    const firstOrgReceiver = await organizationEditPage.page
                        .locator("#orgreceiversettings")
                        .nth(0)
                        .locator("td")
                        .nth(0)
                        .innerText();
                    await organizationEditPage.page
                        .locator("#orgreceiversettings")
                        .getByRole("link", { name: "Edit" })
                        .nth(0)
                        .click();
                    await expect(organizationEditPage.page).toHaveURL(
                        `/admin/orgreceiversettings/org/ignore/receiver/${firstOrgReceiver}/action/edit`,
                    );
                    await expect(organizationEditPage.page.getByText(`Org name: ignore`)).toBeVisible();
                    await expect(
                        organizationEditPage.page.getByText(`Receiver name: ${firstOrgReceiver}`),
                    ).toBeVisible();

                    await organizationEditPage.orgReceiverEdit.cancelButton.click();
                    await expect(organizationEditPage.page).toHaveURL(organizationEditPage.url);
                });
            });
        });
    });

    test.describe("footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({ organizationEditPage }) => {
            await organizationEditPage.testFooter();
        });
    });
});
