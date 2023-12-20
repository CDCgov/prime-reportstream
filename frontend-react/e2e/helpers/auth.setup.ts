import { Page, expect } from "@playwright/test";
import { TOTP } from "otpauth";

import { test as setup } from "./rs-test";
import type { TestLogin } from "./rs-test";

const adminFile = "playwright/.auth/admin.json";

async function logIntoOkta(page: Page, login: TestLogin) {
    const totp = new TOTP({ secret: login.totpCode });

    await page.goto(`/login`);
    await page.getByLabel("Username").fill(login.username);

    const pwd = page.getByLabel("Password");
    // Okta scripting will cause password input to fail if we don't
    // manually focus the field at this point
    await pwd.focus();
    await pwd.fill(login.password);
    await page.getByRole("button", { name: "Sign In" }).click();

    await page.getByLabel("Enter Code ").fill(totp.generate());
    await page.getByRole("button", { name: "Verify" }).click();

    await page.waitForURL("/");
}

setup("authenticate as admin", async ({ page, adminLogin }) => {
    await logIntoOkta(page, adminLogin);
    await expect(page.getByRole("button", { name: "Admin" })).toBeVisible();

    await page.context().storageState({ path: adminFile });
});

// TODO: other user types
/*
const userFile = "playwright/.auth/user.json";

setup("authenticate as user", async ({ page, senderLogin }) => {
    await logIntoOkta(page, senderLogin);

    await page.context().storageState({ path: userFile });
});
*/
