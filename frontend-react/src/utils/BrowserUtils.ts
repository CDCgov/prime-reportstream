import { preferred } from "../browsers.json";

const prefUseragent = new RegExp(preferred.useragent);

export function isUseragentPreferred(ua: string) {
    return prefUseragent.test(ua);
}
