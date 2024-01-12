import { Page, expect } from "@playwright/test";
import { TOTP } from "otpauth";

import { test as setup } from "./rs-test";
import type { TestLogin } from "./rs-test";

const adminFile = "playwright/.auth/admin.json";

async function logIntoOkta(page: Page, login: TestLogin) {
    const totp = new TOTP({ secret: login.totpCode });

    // fulfill GA request so that we don't log to it and alter the metrics
    await page.route("https://www.google-analytics.com/j/collect*", (route) => {
        route.fulfill({
            status: 204,
            body: "",
        });
    });

    await page.goto(`/login`);
    await page.getByLabel("Username").fill(login.username);

    const pwd = page.getByLabel("Password");
    // Okta scripting will cause password input to fail if we don't
    // manually focus the field at this point
    await pwd.focus();
    await pwd.fill(login.password);
    await page.getByRole("button", { name: "Sign in" }).click();

    if (login.totpCode !== "") {
        await page.getByLabel("Enter Code ").fill(totp.generate());
        await page.getByRole("button", { name: "Verify" }).click();
    }

    // Verify we are authenticated
    await expect(page.getByRole("button", { name: "Logout" })).toBeVisible();
}

setup("authenticate as admin", async ({ page, adminLogin }) => {
    await logIntoOkta(page, adminLogin);

    await page.context().storageState({ path: adminFile });
    await page.goto("/admin/settings", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    await expect(page).toHaveURL("/admin/settings");
});

const senderFile = "playwright/.auth/sender.json";

setup("authenticate as sender", async ({ page, senderLogin }) => {
    await logIntoOkta(page, senderLogin);

    await page.context().storageState({ path: senderFile });
    await expect(page).toHaveURL("/submissions");
});

const receiverFile = "playwright/.auth/receiver.json";

setup("authenticate as receiver", async ({ page, receiverLogin }) => {
    await logIntoOkta(page, receiverLogin);

    await page.context().storageState({ path: receiverFile });
    await expect(page).toHaveURL("/");
});
