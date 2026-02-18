import { minimum } from "./browsers.json";

const minUseragent = new RegExp(minimum.useragent);

if (!minUseragent.test(navigator.userAgent)) {
    location.assign("/unsupported-browser.html");
}

export {};
