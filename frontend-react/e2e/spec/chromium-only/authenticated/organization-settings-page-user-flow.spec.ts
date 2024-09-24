import { expect } from "@playwright/test";
import { tableRows } from "../../../helpers/utils";
import { OrganizationPage } from "../../../pages/authenticated/admin/organization";
import { test as baseTest } from "../../../test";


export interface OrganizationPageFixtures {
    organizationPage: OrganizationPage;
}

const test = baseTest.extend<OrganizationPageFixtures>({
    organizationPage: async (
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
        const page = new OrganizationPage({
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

test.describe("Admin Organization Settings Page - user flow smoke tests", {
    tag: "@smoke",
}, () => {
    test.describe("admin user", () => {
        test.use({storageState: "e2e/.auth/admin.json"});

        test.describe("header", () => {
            test("has correct title + heading", async ({organizationPage}) => {
                await organizationPage.testHeader();
            });
        });

        test.describe("table", () => {
            test.beforeEach(async ({organizationPage}) => {
                await organizationPage.page.locator(".usa-table tbody").waitFor({state: "visible"});
            });

            test("has correct headers", async ({organizationPage}) => {
                const result = await organizationPage.testTableHeaders();
                expect(result).toBe(true);
            });

            test("displays data", async ({organizationPage}) => {
                const rowCount = await tableRows(organizationPage.page).count();
                // Heading with result length
                await expect(
                    organizationPage.page.getByRole("heading", {
                        name: `Organizations (${rowCount})`,
                    }),
                ).toBeVisible();
            });

            test("filtering works as expected", async ({organizationPage}) => {
                const table = organizationPage.page.getByRole("table");
                const firstDataRow = organizationPage.page.getByRole("table").getByRole("row").nth(1);
                const firstDataRowName = (await firstDataRow.getByRole("cell").nth(0).textContent()) ?? "INVALID";
                const filterBox = organizationPage.page.getByRole("textbox", {
                    name: "Filter:",
                });

                await expect(filterBox).toBeVisible();

                await filterBox.fill(firstDataRowName);
                const rows = await table.getByRole("row").all();
                expect(rows).toHaveLength(2);
                const cols = rows[1].getByRole("cell").allTextContents();
                const expectedColContents = [
                    await firstDataRow.getByRole("cell").nth(0).textContent(),
                    await firstDataRow.getByRole("cell").nth(1).textContent() ?? "",
                    await firstDataRow.getByRole("cell").nth(2).textContent() ?? "",
                    await firstDataRow.getByRole("cell").nth(3).textContent() ?? "",
                    await firstDataRow.getByRole("cell").nth(4).textContent() ?? "",
                    "SetEdit",
                ];

                for (const [i, col] of (await cols).entries()) {
                    expect(col).toBe(expectedColContents[i]);
                }
            });

            test('selecting "Set" updates link label in navigation', async ({organizationPage}) => {
                const firstDataRow = organizationPage.page.getByRole("table").getByRole("row").nth(1);
                const firstDataRowName = (await firstDataRow.getByRole("cell").nth(0).textContent()) ?? "INVALID";
                const setButton = firstDataRow.getByRole("button", {
                    name: "Set",
                });

                await expect(setButton).toBeVisible();
                await setButton.click();

                const orgLink = organizationPage.page.getByRole("link", {
                    name: firstDataRowName,
                });
                await expect(orgLink).toBeVisible();
                await expect(orgLink).toHaveAttribute("href", "/admin/settings");
            });
        });
    });
});
