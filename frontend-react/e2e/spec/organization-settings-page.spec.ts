import { expect, test } from "@playwright/test";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { mockOrganizationSettingsList } from "../mocks/organizations";
import * as organization from "../pages/organization";

const __dirname = fileURLToPath(import.meta.url);

test.describe("Admin Organization Settings Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await organization.goto(page);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("authenticated receiver", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });
        test("returns Page Not Found", async ({ page }) => {
            await organization.goto(page);
            await expect(page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated sender", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });
        test("returns Page Not Found", async ({ page }) => {
            await organization.goto(page);
            await expect(page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test("If there is an error, the error is shown on the page", async ({
            page,
        }) => {
            await page.route("/api/settings/organizations", (route) =>
                route.fulfill({ status: 500 }),
            );
            await organization.goto(page);
            await expect(page.getByText("there was an error")).toBeVisible();
        });

        test.describe("When there is no error", () => {
            test.beforeEach(async ({ page }) => {
                await page.route(organization.API_ORGANIZATIONS, (route) =>
                    route.fulfill({
                        status: 200,
                        json: mockOrganizationSettingsList,
                    }),
                );
                await organization.goto(page);
            });

            test("nav contains the 'Admin tools' dropdown with 'Organization Settings' option", async ({
                page,
            }) => {
                const navItems = page.locator(".usa-nav  li");
                await expect(navItems).toContainText(["Admin tools"]);

                await page
                    .getByTestId("auth-header")
                    .getByTestId("navDropDownButton")
                    .getByText("Admin tools")
                    .click();

                expect(page.getByText("Organization Settings")).toBeTruthy();

                await page.getByText("Organization Settings").click();
                await expect(page).toHaveURL("/admin/settings");
            });

            test("Has correct title", async ({ page }) => {
                await expect(page).toHaveURL(/settings/);
                await expect(page).toHaveTitle(/Admin-Organizations/);
            });

            test("Displays data", async ({ page }) => {
                // Heading with result length
                await expect(
                    page.getByRole("heading", {
                        name: `Organizations (${mockOrganizationSettingsList.length})`,
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
                const rowCount = mockOrganizationSettingsList.length + 1;
                const table = page.getByRole("table");
                await expect(table).toBeVisible();
                const rows = await table.getByRole("row").all();
                expect(rows).toHaveLength(rowCount);
                for (const [i, row] of rows.entries()) {
                    const cols = await row.getByRole("cell").allTextContents();
                    expect(cols).toHaveLength(colHeaders.length);

                    const { description, jurisdiction, name, stateCode } =
                        i === 0
                            ? mockOrganizationSettingsList[0]
                            : mockOrganizationSettingsList.find(
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
                page,
            }) => {
                const link = page.getByRole("link", {
                    name: "Create New Organization",
                });
                const expectedUrl = "/admin/new/org";

                await expect(link).toBeVisible();
                await link.click();
                await page.waitForURL(expectedUrl);
                await expect(page.getByRole("heading")).toBeVisible();

                expect(page.url()).toContain(expectedUrl);
            });

            test("Save CSV button downloads a file", async ({ page }) => {
                const downloadProm = page.waitForEvent("download");
                const saveButton = page.getByRole("button", {
                    name: "Save List to CSV",
                });

                await expect(saveButton).toBeVisible();
                await saveButton.click();
                const download = await downloadProm;

                const expectedFile = readFileSync(
                    join(__dirname, "../../mocks/prime-orgs.csv"),
                    { encoding: "utf-8" },
                );
                const stream = await download.createReadStream();
                const file = (await stream.toArray()).toString();
                expect(file).toBe(expectedFile);
                expect(download.suggestedFilename()).toBe("prime-orgs.csv");
            });

            test("Filtering works", async ({ page }) => {
                const table = page.getByRole("table");
                const { description, name, jurisdiction, stateCode } =
                    mockOrganizationSettingsList[2];
                const filterBox = page.getByRole("textbox", {
                    name: "Filter:",
                });

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

            test('Clicking "Set" updates link label', async ({ page }) => {
                const firstDataRow = page
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

                const orgLink = page.getByRole("link", {
                    name: firstDataRowName,
                });
                await expect(orgLink).toBeVisible();
                await expect(orgLink).toHaveAttribute(
                    "href",
                    "/admin/settings",
                );
            });

            test("Edit navigation works", async ({ page }) => {
                const firstDataRow = page
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
                await page.waitForURL(expectedUrl);
                await expect(page.getByRole("heading")).toBeVisible();

                expect(page.url()).toContain(expectedUrl);
            });
        });
    });
});
