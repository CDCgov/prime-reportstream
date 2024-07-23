import { expect } from "@playwright/test";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { MOCK_GET_ORGANIZATION_SETTINGS_LIST } from "../../mocks/organizations";
import { OrganizationPage } from "../../pages/organization";
import { test as baseTest } from "../../test";

const __dirname = fileURLToPath(import.meta.url);

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

test.describe("Admin Organization Settings Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ organizationPage }) => {
            await expect(organizationPage.page).toHaveURL("/login");
        });
    });

    test.describe("authenticated receiver", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });
        test("returns Page Not Found", async ({ organizationPage }) => {
            await expect(organizationPage.page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated sender", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });
        test("returns Page Not Found", async ({ organizationPage }) => {
            await expect(organizationPage.page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test("If there is an error, the error is shown on the page", async ({
            organizationPage,
        }) => {
            organizationPage.mockError = true;
            await organizationPage.reload();
            await expect(
                organizationPage.page.getByText("there was an error"),
            ).toBeVisible();
        });

        test.describe(
            "When there is no error",
            {
                tag: "@smoke",
            },
            () => {
                test("nav contains the 'Admin tools' dropdown with 'Organization Settings' option", async ({
                    organizationPage,
                }) => {
                    const navItems =
                        organizationPage.page.locator(".usa-nav  li");
                    await expect(navItems).toContainText(["Admin tools"]);

                    await organizationPage.page
                        .getByTestId("auth-header")
                        .getByTestId("navDropDownButton")
                        .getByText("Admin tools")
                        .click();

                    expect(
                        organizationPage.page.getByText(
                            "Organization Settings",
                        ),
                    ).toBeTruthy();

                    await organizationPage.page
                        .getByText("Organization Settings")
                        .click();
                    await expect(organizationPage.page).toHaveURL(
                        "/admin/settings",
                    );
                });

                test("Has correct title", async ({ organizationPage }) => {
                    await expect(organizationPage.page).toHaveURL(/settings/);
                    await expect(organizationPage.page).toHaveTitle(
                        /Admin-Organizations/,
                    );
                });

                test("Displays data", async ({ organizationPage }) => {
                    // Heading with result length
                    await expect(
                        organizationPage.page.getByRole("heading", {
                            name: `Organizations (${MOCK_GET_ORGANIZATION_SETTINGS_LIST.length})`,
                        }),
                    ).toBeVisible();

                    // Table
                    // empty is button column
                    const colHeaders = [
                        "Name",
                        "Description",
                        "Jurisdiction",
                        "State",
                        "County",
                        "",
                    ];
                    // include header row
                    const rowCount =
                        MOCK_GET_ORGANIZATION_SETTINGS_LIST.length + 1;
                    const table = organizationPage.page.getByRole("table");
                    await expect(table).toBeVisible();
                    const rows = await table.getByRole("row").all();
                    expect(rows).toHaveLength(rowCount);
                    for (const [i, row] of rows.entries()) {
                        const cols = await row
                            .getByRole("cell")
                            .allTextContents();
                        expect(cols).toHaveLength(colHeaders.length);

                        const { description, jurisdiction, name, stateCode } =
                            i === 0
                                ? MOCK_GET_ORGANIZATION_SETTINGS_LIST[0]
                                : MOCK_GET_ORGANIZATION_SETTINGS_LIST.find(
                                      (i) => i.name === cols[0],
                                  ) ?? { name: "INVALID" };
                        // if first row, we expect column headers. else, the data row matching id (name)
                        // SetEdit is text of buttons in button column
                        const expectedColContents =
                            i === 0
                                ? colHeaders
                                : [
                                      name,
                                      description ?? "",
                                      jurisdiction ?? "",
                                      stateCode ?? "",
                                      "",
                                      "SetEdit",
                                  ];

                        for (const [i, col] of cols.entries()) {
                            expect(col).toBe(expectedColContents[i]);
                        }
                    }
                });

                test("Create new organization navigation works", async ({
                    organizationPage,
                }) => {
                    const link = organizationPage.page.getByRole("link", {
                        name: "Create New Organization",
                    });
                    const expectedUrl = "/admin/new/org";

                    await expect(link).toBeVisible();
                    await link.click();
                    await organizationPage.page.waitForURL(expectedUrl);
                    await expect(
                        organizationPage.page.getByRole("heading"),
                    ).toBeVisible();

                    expect(organizationPage.page.url()).toContain(expectedUrl);
                });

                test("Save CSV button downloads a file", async ({
                    organizationPage,
                }) => {
                    const downloadProm =
                        organizationPage.page.waitForEvent("download");
                    const saveButton = organizationPage.page.getByRole(
                        "button",
                        {
                            name: "Save List to CSV",
                        },
                    );

                    await expect(saveButton).toBeVisible();
                    await saveButton.click();
                    const download = await downloadProm;

                    const expectedFile = readFileSync(
                        join(__dirname, "../../../mocks/prime-orgs.csv"),
                        { encoding: "utf-8" },
                    );
                    const stream = await download.createReadStream();
                    const file = (await stream.toArray()).toString();
                    expect(file).toBe(expectedFile);
                    expect(download.suggestedFilename()).toBe("prime-orgs.csv");
                });

                test("Filtering works", async ({ organizationPage }) => {
                    const table = organizationPage.page.getByRole("table");
                    const { description, name, jurisdiction, stateCode } =
                        MOCK_GET_ORGANIZATION_SETTINGS_LIST[2];
                    const filterBox = organizationPage.page.getByRole(
                        "textbox",
                        {
                            name: "Filter:",
                        },
                    );

                    await expect(filterBox).toBeVisible();

                    await filterBox.fill(name);
                    const rows = await table.getByRole("row").all();
                    expect(rows).toHaveLength(2);
                    const cols = rows[1].getByRole("cell").allTextContents();
                    const expectedColContents = [
                        name,
                        description ?? "",
                        jurisdiction ?? "",
                        stateCode ?? "",
                        "",
                        "SetEdit",
                    ];

                    for (const [i, col] of (await cols).entries()) {
                        expect(col).toBe(expectedColContents[i]);
                    }
                });

                test('Clicking "Set" updates link label', async ({
                    organizationPage,
                }) => {
                    const firstDataRow = organizationPage.page
                        .getByRole("table")
                        .getByRole("row")
                        .nth(1);
                    const firstDataRowName =
                        (await firstDataRow
                            .getByRole("cell")
                            .nth(0)
                            .textContent()) ?? "INVALID";
                    const setButton = firstDataRow.getByRole("button", {
                        name: "Set",
                    });

                    await expect(setButton).toBeVisible();
                    await setButton.click();

                    const orgLink = organizationPage.page.getByRole("link", {
                        name: firstDataRowName,
                    });
                    await expect(orgLink).toBeVisible();
                    await expect(orgLink).toHaveAttribute(
                        "href",
                        "/admin/settings",
                    );
                });

                test("Edit navigation works", async ({ organizationPage }) => {
                    const firstDataRow = organizationPage.page
                        .getByRole("table")
                        .getByRole("row")
                        .nth(1);
                    const firstDataRowName = await firstDataRow
                        .getByRole("cell")
                        .nth(0)
                        .textContent();
                    const expectedUrl = `/admin/orgsettings/org/${firstDataRowName}`;
                    const editButton = firstDataRow.getByRole("button", {
                        name: "Edit",
                    });

                    await expect(editButton).toBeVisible();
                    await editButton.click();
                    await organizationPage.page.waitForURL(expectedUrl);
                    await expect(
                        organizationPage.page.getByRole("heading"),
                    ).toBeVisible();

                    expect(organizationPage.page.url()).toContain(expectedUrl);
                });
            },
        );
    });
});
