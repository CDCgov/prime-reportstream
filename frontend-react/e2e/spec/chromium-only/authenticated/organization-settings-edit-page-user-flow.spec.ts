import { expect } from "@playwright/test";
import { OrganizationEditPage } from "../../../pages/authenticated/admin/organization-edit";
import { test as baseTest } from "../../../test";

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

test.describe(
    "Organization Edit Page",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("admin user", () => {
            test.use({ storageState: "e2e/.auth/admin.json" });

            test("has correct title", async ({ organizationEditPage }) => {
                await organizationEditPage.testHeader(false);
                await expect(organizationEditPage.page.getByText(/Org name: ignore/)).toBeVisible();
            });

            test.describe("edit section", () => {
                test("has expected 'Meta'", async ({ organizationEditPage }) => {
                    const meta = organizationEditPage.page.getByTestId("gridContainer").getByTestId("grid").nth(2);
                    await expect(meta).not.toBeEmpty();
                });

                test("has expected 'Description'", async ({ organizationEditPage }) => {
                    await expect(organizationEditPage.page.getByTestId("description")).not.toBeEmpty();
                });

                test("has expected 'Jurisdiction'", async ({ organizationEditPage }) => {
                    await expect(organizationEditPage.page.getByTestId("jurisdiction")).not.toBeEmpty();
                });
            });

            test.describe("'Organization Sender Settings' section", () => {
                test.beforeEach(async ({ organizationEditPage }) => {
                    await organizationEditPage.page
                        .locator("#orgsendersettings .usa-table tbody")
                        .waitFor({ state: "visible" });
                });

                test("has at least one sender listed in the table", async ({ organizationEditPage }) => {
                    const rowCount = await organizationEditPage.page
                        .locator("#orgsendersettings .usa-table tbody tr")
                        .count();
                    expect(rowCount).toBeGreaterThanOrEqual(1);
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

                    await expect(organizationEditPage.page.getByTestId("name")).not.toBeEmpty();
                    await expect(organizationEditPage.page.getByTestId("format")).not.toBeEmpty();
                    await expect(organizationEditPage.page.getByTestId("topic")).not.toBeEmpty();
                    await expect(organizationEditPage.page.getByTestId("customerStatus")).not.toBeEmpty();
                    await expect(organizationEditPage.page.getByTestId("processingType")).not.toBeEmpty();
                });
            });

            test.describe("'Organization Receiver Settings' section", () => {
                test.beforeEach(async ({ organizationEditPage }) => {
                    await organizationEditPage.page
                        .locator("#orgreceiversettings .usa-table tbody")
                        .waitFor({ state: "visible" });
                });

                test("has at least one sender listed in the table", async ({ organizationEditPage }) => {
                    const rowCount = await organizationEditPage.page
                        .locator("#orgreceiversettings .usa-table tbody tr")
                        .count();
                    expect(rowCount).toBeGreaterThanOrEqual(1);
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

                    await expect(organizationEditPage.page.getByTestId("name")).not.toBeEmpty();
                    await expect(organizationEditPage.page.getByTestId("topic")).not.toBeEmpty();
                    await expect(organizationEditPage.page.getByTestId("customerStatus")).not.toBeEmpty();
                    await expect(organizationEditPage.page.getByTestId("translation")).not.toBeEmpty();
                });
            });
        });
    },
);
