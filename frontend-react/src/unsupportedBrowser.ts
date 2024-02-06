import { minimumBrowsersRegex } from "./utils/SupportedBrowsers";

if (!minimumBrowsersRegex.test(navigator.userAgent)) {
    // eslint-disable-next-line no-restricted-globals
    location.assign("/unsupported-browser.html");
}

export {};
