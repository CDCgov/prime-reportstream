import { expect } from "@playwright/test";
import process from "node:process";

import { OrganizationPage } from "../../pages/authenticated/admin/organization";
import { test as baseTest } from "../../test";

const timeout = parseInt(process.env.VITE_IDLE_TIMEOUT ?? "900000");
const timeoutLow = timeout - 1000;
const timeoutHigh = timeout + 1000;

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
        await page.page.clock.install();
        await page.goto();
        await use(page);
    },
});

test.use({ storageState: "e2e/.auth/admin.json" });

test.describe("Idle time out", () => {
    test("Does not trigger early", async ({ organizationPage }) => {
        await expect(organizationPage.page.getByRole("banner").first()).toBeVisible();
        await organizationPage.page.keyboard.down("Tab");

        await organizationPage.page.clock.fastForward(timeoutLow);

        await expect(organizationPage.page.getByRole("banner").first()).toBeVisible();
    });

    test("Triggers on time", async ({ organizationPage }) => {
        await expect(organizationPage.page.getByRole("banner").first()).toBeVisible();
        await organizationPage.page.keyboard.down("Tab");

        await organizationPage.page.clock.fastForward(timeoutHigh);

        await expect(organizationPage.page.getByRole("link", { name: "Login" })).toBeVisible();
    });
});
