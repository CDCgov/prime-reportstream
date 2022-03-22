import { parseOrgs } from "./SessionStorageTools";

test("parseOrgs", () => {
    const parsed = parseOrgs([
        "DHaz_phd",
        "DHPrimeAdmin",
        "DHSender_ignore.ignore-waters",
        "DHSender_ignore",
    ]);
    expect(parsed).toEqual([
        { org: "az-phd", senderName: undefined },
        { org: "PrimeAdmin", senderName: undefined },
        { org: "ignore", senderName: "ignore-waters" },
        { org: "ignore", senderName: "default" },
    ]);
});
