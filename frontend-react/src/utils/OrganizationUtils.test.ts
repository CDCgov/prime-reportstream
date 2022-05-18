import {
    getOktaGroups,
    getRSOrgs,
    groupToOrg,
    parseOrgs,
    RSOrgType,
    RSUserClaims,
} from "./OrganizationUtils";
import { mockToken } from "./TestUtils";

const badAccessToken = mockToken();
const goodAccessToken = mockToken({
    claims: {
        organization: [
            "DHPrimeAdmins",
            "DHignoreAdmins",
            "DHSender_ignore",
            "DHxx_phd",
            "DHSender_ignore.ignore-waters",
        ],
    } as RSUserClaims,
});

test("groupToOrg", () => {
    const admins = groupToOrg("DHPrimeAdmins");
    const ignoreWaters = groupToOrg("DHSender_ignore.ignore-waters");
    const mdPhd = groupToOrg("DHmd_phd");
    const simpleReport = groupToOrg("simple_report");
    const malformedGroupName = groupToOrg("DHSender_test_org");
    const multipleUnderscoresName = groupToOrg("DHxx_yyy_phd");
    const undefinedOrg = groupToOrg(undefined);

    expect(admins).toBe("PrimeAdmins");
    expect(ignoreWaters).toBe("ignore.ignore-waters");
    expect(mdPhd).toBe("md-phd");
    expect(simpleReport).toBe("simple_report");
    expect(malformedGroupName).toBe("test_org");
    expect(multipleUnderscoresName).toBe("xx-yyy-phd");
    expect(undefinedOrg).toBe("");
});

test("getOktaGroups", () => {
    const org = getOktaGroups(goodAccessToken);
    const noOrg = getOktaGroups(badAccessToken);
    expect(org).toEqual([
        "DHPrimeAdmins",
        "DHignoreAdmins",
        "DHSender_ignore",
        "DHxx_phd",
        "DHSender_ignore.ignore-waters",
    ]);
    expect(noOrg).toEqual([]);
});

test("getRSOrgs", () => {
    const all = getRSOrgs(goodAccessToken);
    const senders = getRSOrgs(goodAccessToken, RSOrgType.SENDER);
    const receivers = getRSOrgs(goodAccessToken, RSOrgType.RECEIVER);
    const admins = getRSOrgs(goodAccessToken, RSOrgType.ADMIN);

    expect(all).toEqual([
        "PrimeAdmins",
        "ignoreAdmins", // Currently not a real use case!
        "ignore",
        "xx-phd",
        "ignore.ignore-waters",
    ]);
    expect(senders).toEqual(["ignore", "ignore.ignore-waters"]);
    expect(receivers).toEqual(["xx-phd"]);
    expect(admins).toEqual(["PrimeAdmins", "ignoreAdmins"]);
});

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
