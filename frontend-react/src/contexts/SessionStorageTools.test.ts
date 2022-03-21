import { parseOrgs } from "./SessionStorageTools";

test("parseOrgs", () => {
    const parsed = parseOrgs([
        "DHaz_phd",
        "DHPrimeAdmin",
        "DHSender_ignore.ignore-waters",
        "DHSender_ignore",
    ]);
    expect(parsed).toEqual([
        { org: "az-phd", sender: undefined },
        { org: "PrimeAdmin", sender: undefined },
        { org: "ignore", sender: "ignore-waters" },
        { org: "ignore", sender: "default" },
    ]);
});
