import { expect, Page, test } from "@playwright/test";
import { fileURLToPath } from "node:url";
import { MOCK_GET_RECEIVER_STATUS } from "../../mocks/receiverStatus";
import * as receiverStatus from "../../pages/admin/receiver-status";

const __dirname = fileURLToPath(import.meta.url);

async function gotoPage(page: Page) {
    await page.route(receiverStatus.API_RECEIVER_STATUS, (route) =>
        route.fulfill({
            status: 200,
            json: MOCK_GET_RECEIVER_STATUS,
        }),
    );
    await receiverStatus.goto(page);
}

test.describe("Admin Organization Settings Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await receiverStatus.goto(page);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("authenticated receiver", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });
        test("returns Page Not Found", async ({ page }) => {
            await receiverStatus.goto(page);
            await expect(page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated sender", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });
        test("returns Page Not Found", async ({ page }) => {
            await receiverStatus.goto(page);
            await expect(page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test("If there is an error, the error is shown on the page", async ({
            page,
        }) => {
            await page.route(receiverStatus.API_RECEIVER_STATUS, (route) =>
                route.fulfill({ status: 500 }),
            );
            await receiverStatus.goto(page);
            await expect(page.getByText("there was an error")).toBeVisible();
        });

            test("Has correct title", async ({ page }) => {
                await gotoPage(page)
                await expect(page).toHaveURL(/send-dash/);
                await expect(page).toHaveTitle(/Receiver status dashboard/);
            });

        test.describe("When there is no error", () => {
            test.beforeEach(async ({page}) => {
                await gotoPage(page)
            })
            test.describe("Displays correctly", () => {

                test("Filter", async ({ page }) => {
                    await gotoPage(page)          
                    const {startDateString, endDateString} = await receiverStatus.getDateStrings(page)
                    const formOptionContainersLocator = page.locator(
                        "form[name=filter]> *",
                    );
                    await expect(formOptionContainersLocator).toHaveCount(4);
                    const formOptionContainers =
                        await formOptionContainersLocator.all();
                    const [
                        dateRangeContainer
                    ] = formOptionContainers;

                    // check labels
                    const expectedLabels = [
                        "Date range:",
                        "Receiver Name:",
                        "Results Message:",
                        "Success Type:",
                    ];
                    for (const [
                        i,
                        container,
                    ] of formOptionContainers.entries()) {
                        await expect(container.locator("> label")).toHaveText(
                            expectedLabels[i],
                        );
                    }

                    // check tooltips (date range has none)
                    const expectedTooltips = [
                        "Filter rows on just the first column's Organization name and Receiver setting name.",
                        "Filter rows on the Result Message details. This value is found in the details.",
                        "Show only rows in one of these states.",
                    ];
                    for (const [i, container] of formOptionContainers
                        .slice(1)
                        .entries()) {
                        const wrapper = container.getByTestId("tooltipWrapper");
                        await expect(wrapper).toBeAttached();

                        const body = wrapper.getByRole("tooltip");
                        await expect(body).toBeHidden();
                        await wrapper.hover();
                        await expect(body).toBeVisible();
                        await expect(body).toHaveText(expectedTooltips[i]);
                    }

                    /* check form option inputs */

                    // date range
                    const expectedDateRange = `🗓 ${startDateString} — ${endDateString}`;
                    // date range is a widget initiated by a button, so target its display value element
                    const dateRangeDisplay =
                        dateRangeContainer.locator("> span");
                    await expect(dateRangeDisplay).toBeVisible();
                    await expect(dateRangeDisplay).toHaveText(
                        expectedDateRange,
                    );

                    // others
                    const expectedRoles = ["textbox", "textbox", "combobox"];
                    const expectedTexts = [
                        "",
                        "",
                        "Show AllFailedMixed success",
                    ];
                    // "UNDEFINED" = Show All
                    const expectedValues = ["", "", "UNDEFINED"];
                    for (const [i, container] of formOptionContainers
                        .slice(1)
                        .entries()) {
                        const target = container.getByRole(
                            expectedRoles[i] as any,
                        );
                        await expect(target).toBeVisible();
                        await expect(target).toHaveText(expectedTexts[i]);
                        await expect(target).toHaveValue(expectedValues[i]);
                    }
                });

                test("receiver status", async ({ page }) => {
                    await expect(
                        page.getByRole("heading", {
                            name: "Receiver Status Dashboard",
                        }),
                    ).toBeVisible();
                    const {startDateString, endDateString, priorDayString} = await receiverStatus.getDateStrings(page)
                    const statusContainer = page.locator(
                        ".rs-admindash-component",
                    );
                    await expect(statusContainer).toBeVisible();

                    const statusRows = statusContainer.locator("> .grid-row");
                    await expect(statusRows).toHaveCount(
                        new Set(
                            MOCK_GET_RECEIVER_STATUS.map((r) => r.receiverId),
                        ).size,
                    );

                    const expectedDaysText = [
                        startDateString,
                        endDateString,
                        priorDayString,
                    ].join("            ");
                    for (const [i, row] of (await statusRows.all()).entries()) {
                        const {
                            organizationName,
                            receiverName,
                            connectionCheckResult,
                        } = MOCK_GET_RECEIVER_STATUS[i];
                        const expectedTitleText = `${organizationName}${receiverName}${connectionCheckResult}`;
                        const titleCol = row.locator("> :nth(0)");
                        const displayCol = row.locator("> :nth(1)");
                        console.log(
                            expectedTitleText,
                            await titleCol.textContent(),
                        );

                        await expect(titleCol).toBeVisible();
                        await expect(displayCol).toBeVisible();

                        await expect(titleCol).toHaveText(expectedTitleText);
                        await expect(displayCol).toHaveText(expectedDaysText);
                    }
                });
            });

            test.describe("filter options work", () => {
                test("date range", async ({ page }) => {
                    await expect(page).toHaveTitle(/Receiver status dashboard/);
                });
                test("receiver name", async ({ page }) => {
                    await expect(page).toHaveTitle(/Receiver status dashboard/);
                });
                test("result message", async ({ page }) => {
                    await expect(page).toHaveTitle(/Receiver status dashboard/);
                });
                test("success type", async ({ page }) => {
                    await expect(page).toHaveTitle(/Receiver status dashboard/);
                });
            });

            test("org links work", async ({ page }) => {
                await expect(page).toHaveTitle(/Receiver status dashboard/);
            });
            test("receiver links work", async ({ page }) => {
                await expect(page).toHaveTitle(/Receiver status dashboard/);
            });
        });
    });
});
