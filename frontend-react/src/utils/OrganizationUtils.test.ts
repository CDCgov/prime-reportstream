import {
    AccessTokenWithRSClaims,
    getOktaGroups,
    parseOrgName,
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
    },
} as AccessTokenWithRSClaims);

test("groupToOrg", () => {
    const admins = parseOrgName("DHPrimeAdmins");
    const ignoreWaters = parseOrgName("DHSender_ignore.ignore-waters");
    const mdPhd = parseOrgName("DHmd_phd");
    const simpleReport = parseOrgName("simple_report");
    const malformedGroupName = parseOrgName("DHSender_test_org");
    const multipleUnderscoresName = parseOrgName("DHxx_yyy_phd");
    const undefinedOrg = parseOrgName(undefined);

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
