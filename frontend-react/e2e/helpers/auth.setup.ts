import { expect, Page } from "@playwright/test";
import { TOTP } from "otpauth";

import { test as setup } from "./rs-test";
import type { TestLogin } from "./rs-test";

async function logIntoOkta(page: Page, login: TestLogin) {
    const totp = new TOTP({ secret: login.totpCode });

    // fulfill GA request so that we don't log to it and alter the metrics
    await page.route("https://www.google-analytics.com/j/collect*", (route) =>
        route.fulfill({ status: 204, body: "" }),
    );

    await page.goto("/login", {
        waitUntil: "domcontentloaded",
    });
    await page.getByLabel("Username").fill(login.username);

    const pwd = page.getByLabel("Password");
    // Okta scripting will cause password input to fail if we don't
    // manually focus the field at this point
    await pwd.focus();
    await pwd.fill(login.password);
    await page.getByRole("button", { name: "Sign in" }).click();

    if (login.totpCode !== "" && login.totpCode !== undefined) {
        await page.getByLabel("Enter Code ").fill(totp.generate());
        await page.getByRole("button", { name: "Verify" }).click();
    }

    await page.waitForLoadState("domcontentloaded");

    // Verify we are authenticated
    await expect(page.getByRole("button", { name: "Logout" })).toBeVisible();
}

/**
 * Create sessions for all authentication scenarios. Real tests on login behavior should
 * go into a dedicated test file.
 */
setup(
    "create authenticated sessions",
    async ({ page, adminLogin, senderLogin, receiverLogin }) => {
        for (const login of [adminLogin, senderLogin, receiverLogin]) {
            await logIntoOkta(page, login);

            const logoutBtn = page.getByRole("button", { name: "Logout" });
            await page.context().storageState({ path: login.path });

            await logoutBtn.click();
            await expect(
                page.getByRole("link", { name: "Login" }),
            ).toBeAttached();
        }
    },
);
