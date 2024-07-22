import { Page } from "@playwright/test";
import { TOTP } from "otpauth";

import { fulfillGoogleAnalytics } from "./utils";
import { expect, test as setup, type TestLogin } from "../test";

async function logIntoOkta(page: Page, login: TestLogin) {
    const totp = new TOTP({ secret: login.totpCode });

    // fulfill GA request so that we don't log to it and alter the metrics
    await fulfillGoogleAnalytics(page);

    // block AI
    await page.route("**/v2/track", (route) => route.abort("blockedbyclient"));

    await page.goto("/login", {
        waitUntil: "domcontentloaded",
    });
    await page
        .getByLabel("Username")
        .or(page.getByLabel("Username or email"))
        .fill(login.username);

    const btnNext = page.getByRole("button", { name: "Next" });
    if (btnNext) {
        await btnNext.click();
    }

    const pwd = page.getByLabel("Password");
    // Okta scripting will cause password input to fail if we don't
    // manually focus the field at this point
    await pwd.focus();
    await pwd.fill(login.password);
    const btnSubmit = page
        .getByRole("button", { name: "Sign in" })
        .or(page.getByRole("button", { name: "Verify" }));
    await btnSubmit.click();

    await expect(btnSubmit).not.toBeAttached();

    const totpSelect = page.getByLabel("Select Google Authenticator.");
    if (await totpSelect.isVisible()) {
        await totpSelect.click();
    }

    if (login.totpCode !== "" && login.totpCode !== undefined) {
        await page.getByLabel("Enter Code ").fill(totp.generate());
        await page.getByRole("button", { name: "Verify" }).click();
    }

    await page.waitForLoadState("domcontentloaded");

    // Verify we are authenticated
    await expect(page.getByTestId("logout")).toBeVisible();
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

            await page.context().storageState({ path: login.path });
            await page.getByTestId("logout").click();

            await expect(
                page.getByRole("link", { name: "Login" }),
            ).toBeAttached();
        }
    },
);
