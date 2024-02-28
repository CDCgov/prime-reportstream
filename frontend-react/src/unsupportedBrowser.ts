import { minimum } from "./browsers.json";

const minUseragent = new RegExp(minimum.useragent);

if (!minUseragent.test(navigator.userAgent)) {
    // eslint-disable-next-line no-restricted-globals
    location.assign("/unsupported-browser.html");
}

export {};
