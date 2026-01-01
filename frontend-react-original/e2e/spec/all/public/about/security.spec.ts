import { SecurityPage } from "../../../../pages/public/about/security";
import { test as baseTest, expect } from "../../../../test";

const URL_SECURITY = "/about/security";

export interface SecurityPageFixtures {
    securityPage: SecurityPage;
}

const test = baseTest.extend<SecurityPageFixtures>({
    securityPage: async (
        {
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        },
        use,
    ) => {
        const page = new SecurityPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        });
        await page.goto();
        await use(page);
    },
});

test.describe(
    "Security page",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("Header", () => {
            test("has correct title + heading", async ({ securityPage }) => {
                await securityPage.testHeader();
            });
        });

        test("nav contains the 'About' dropdown with 'Security Reportstream' option", async ({ securityPage }) => {
            const navItems = securityPage.page.locator(".usa-nav  li");
            await expect(navItems).toContainText(["About"]);

            await securityPage.page.getByTestId("header").getByTestId("navDropDownButton").getByText("About").click();

            await securityPage.page.getByTestId("header").getByRole("link", { name: "Security" }).click();
            await expect(securityPage.page).toHaveURL(URL_SECURITY);
        });

        test.describe("Security section", () => {
            test("Accordion sections expand", async ({ securityPage }) => {
                // Not necessary to test all expansions.
                const accordionCol = [
                    "Does ReportStream comply with the Federal Information Security Modernization Act (FISMA)?",
                    "Is ReportStream FedRAMP approved?",
                ];

                for (let i = 0; i < accordionCol.length; i++) {
                    const accordionItem = `accordionItem_${i + 1}--content`;
                    await expect(securityPage.page.getByTestId(accordionItem)).toBeHidden();

                    await securityPage.page
                        .getByRole("button", {
                            name: accordionCol[i],
                        })
                        .click();

                    await expect(securityPage.page.getByTestId(accordionItem)).toBeVisible();

                    await securityPage.page
                        .getByRole("button", {
                            name: accordionCol[i],
                        })
                        .click();

                    await expect(securityPage.page.getByTestId(accordionItem)).toBeHidden();
                }
            });
        });

        test.describe("Footer", () => {
            test("has footer + test bottom-to-top page scroll", async ({ securityPage }) => {
                await securityPage.testFooter();
            });
        });
    },
);
