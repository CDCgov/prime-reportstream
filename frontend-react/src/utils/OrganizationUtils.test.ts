import { AccessToken } from "@okta/okta-auth-js";

import {
    getOktaGroups,
    getRSOrgs,
    groupToOrg,
    parseOrgs,
    RSOrgType,
    RSUserClaims,
} from "./OrganizationUtils";

const badAccessToken: AccessToken = {
    authorizeUrl: "",
    expiresAt: 0,
    scopes: [],
    userinfoUrl: "",
    accessToken: "",
    claims: {
        sub: "", // This satisfies the required attribute
    },
    tokenType: "",
};

const goodAccessToken: AccessToken = {
    authorizeUrl: "",
    expiresAt: 0,
    scopes: [],
    userinfoUrl: "",
    accessToken: "",
    claims: {
        organization: [
            "DHPrimeAdmins",
            "DHignoreAdmins",
            "DHSender_ignore",
            "DHxx_phd",
            "DHSender_ignore.ignore-waters",
        ],
    } as RSUserClaims,
    tokenType: "",
};

test("groupToOrg", () => {
    const admins = groupToOrg("DHPrimeAdmins");
    const ignoreWaters = groupToOrg("DHSender_ignore.ignore-waters");
    const mdPhd = groupToOrg("DHmd_phd");
    const simpleReport = groupToOrg("simple_report");
    const malformedGroupName = groupToOrg("DHSender_test_org");

    expect(admins).toBe("PrimeAdmins");
    expect(ignoreWaters).toBe("ignore.ignore-waters");
    expect(mdPhd).toBe("md-phd");
    expect(simpleReport).toBe("simple_report");
    expect(malformedGroupName).toBe("test_org");
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
    const senders = getRSOrgs(goodAccessToken, false, RSOrgType.SENDER);
    const receivers = getRSOrgs(goodAccessToken, false, RSOrgType.RECEIVER);
    const admins = getRSOrgs(goodAccessToken, false, RSOrgType.ADMIN);
    const first = getRSOrgs(goodAccessToken, true);

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
    expect(first).toEqual("PrimeAdmins");
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
