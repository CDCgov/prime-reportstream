import { expect } from "@playwright/test";
import process from "node:process";

import { OrganizationPage } from "../../pages/organization";
import { test as baseTest } from "../../test";

const timeout = parseInt(process.env.VITE_IDLE_TIMEOUT ?? "20000");
// Add/Sub 500 ms to account for variance
const timeoutLow = timeout - 500;
const timeoutHigh = timeout + 500;

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
            isFrontendWarningsLog
        });
        await page.goto();
        await use(page);
    },
});

test.use({ storageState: "e2e/.auth/admin.json" });

test.skip("Does not trigger early", async ({ organizationPage }) => {
    await expect(
        organizationPage.page.getByRole("banner").first(),
    ).toBeVisible();
    await organizationPage.page.keyboard.down("Tab");

    const start = new Date();

    await organizationPage.page.waitForRequest(
        /\/oauth2\/default\/v1\/revoke/,
        {
            timeout: timeoutHigh,
        },
    );

    const end = new Date();

    const idleTime = Math.abs(end.valueOf() - start.valueOf());

    expect(idleTime).not.toBeLessThan(timeoutLow);
    expect(idleTime).not.toBeGreaterThan(timeoutHigh);
});
