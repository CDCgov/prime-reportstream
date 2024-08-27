import { endOfDay, startOfDay } from "date-fns";
import { test as baseTest, expect } from "../../test";

const test = baseTest.extend({});

test("playwright/browser timezone parity", async ({ page }) => {
    const now = new Date();
    const browserNowIso = await page.evaluate(() => new Date().toISOString());
    const browserNow = new Date(browserNowIso);

    const timezoneId = Intl.DateTimeFormat().resolvedOptions().timeZoneName;
    const browserTimezoneId = await page.evaluate(() => Intl.DateTimeFormat().resolvedOptions().timeZoneName);

    const nowStart = startOfDay(now);
    const browserStartIso = await page.evaluate(() => {
        const d = new Date();
        d.setHours(0, 0, 0, 0);
        return d.toISOString();
    });
    const browserStart = new Date(browserStartIso);

    const nowEnd = endOfDay(now);
    const browserEndIso = await page.evaluate(() => {
        const d = new Date();
        d.setHours(23, 59, 59, 999);
        return d.toISOString();
    });
    const browserEnd = new Date(browserEndIso);

    expect(now.getTimezoneOffset()).toBe(browserNow.getTimezoneOffset());
    expect(timezoneId).toBe(browserTimezoneId);
    expect(nowStart.toLocaleString()).toBe(browserStart.toLocaleString());
    expect(nowEnd.toLocaleString()).toBe(browserEnd.toLocaleString());
});
