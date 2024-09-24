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

test.describe("Organization Edit Page", {
    tag: "@smoke",
}, () => {
    test.describe("admin user", () => {
        test.use({storageState: "e2e/.auth/admin.json"});

        test("has correct title", async ({organizationEditPage}) => {
            await organizationEditPage.testHeader(false);
            await expect(organizationEditPage.page.getByText(/Org name: ignore/)).toBeVisible();
        });

        test.describe("edit section", () => {
            test("has expected 'Meta'", async ({organizationEditPage}) => {
                const meta = organizationEditPage.page.getByTestId("gridContainer").getByTestId("grid").nth(2);
                 await expect(meta).not.toBeEmpty();
            });

            test("has expected 'Description'", async ({organizationEditPage}) => {
                 await expect(organizationEditPage.page.getByTestId("description")).not.toBeEmpty();
            });

            test("has expected 'Jurisdiction'", async ({organizationEditPage}) => {
                await expect(organizationEditPage.page.getByTestId("jurisdiction")).not.toBeEmpty();
            });
        });

        test.describe("'Organization Sender Settings' section", () => {
            test.beforeEach(async ({ organizationEditPage }) => {
                await organizationEditPage.page.locator("#orgsendersettings .usa-table tbody").waitFor({ state: "visible" });
            });

            test("has at least one sender listed in the table", async ({organizationEditPage}) => {
                const rowCount = await organizationEditPage.page.locator("#orgsendersettings .usa-table tbody tr").count();
                expect(rowCount).toBeGreaterThanOrEqual(1);
            });
        });

        test.describe("'Organization Receiver Settings' section", () => {
            test.beforeEach(async ({ organizationEditPage }) => {
                await organizationEditPage.page.locator("#orgreceiversettings .usa-table tbody").waitFor({ state: "visible" });
            });

            test("has at least one sender listed in the table", async ({organizationEditPage}) => {
                const rowCount = await organizationEditPage.page.locator("#orgreceiversettings .usa-table tbody tr").count();
                expect(rowCount).toBeGreaterThanOrEqual(1);
            });
        });
    });
});
