import { expect, test } from "@playwright/test";
import process from "node:process";

const timeout = parseInt(process.env.VITE_IDLE_TIMEOUT ?? "20000");
// Add/Sub 500 ms to account for variance
const timeoutLow = timeout - 500;
const timeoutHigh = timeout + 500;

test.use({ storageState: "playwright/.auth/admin.json" });

test.skip("Does not trigger early", async ({ page }) => {
    await page.goto("/admin/settings");

    await expect(page.getByRole("banner").first()).toBeVisible();
    await page.keyboard.down("Tab");

    const start = new Date();

    await page.waitForRequest(/\/oauth2\/default\/v1\/revoke/, {
        timeout: timeoutHigh,
    });

    const end = new Date();

    const idleTime = Math.abs(end.valueOf() - start.valueOf());

    expect(idleTime).not.toBeLessThan(timeoutLow);
    expect(idleTime).not.toBeGreaterThan(timeoutHigh);
});
