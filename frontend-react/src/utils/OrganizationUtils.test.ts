import {
    AccessTokenWithRSClaims,
    getOktaGroups,
    membershipsFromToken,
    MemberType,
    parseOrgName,
} from "./OrganizationUtils";
import { mockAccessToken } from "./TestUtils";

const mockGetSessionMembershipState = vi.fn();
const mockGetOrganizationOverride = vi.fn();

const mockStoreSessionMembershipState = vi.fn();
const mockStoreOrganizationOverride = vi.fn();
const mockUpdateApiSessions = vi.fn();

vi.mock("../utils/SessionStorageTools", () => {
    return {
        storeSessionMembershipState: (value: string) =>
            mockStoreSessionMembershipState(value),
        getSessionMembershipState: () => mockGetSessionMembershipState(),
        storeOrganizationOverride: (value: string) =>
            mockStoreOrganizationOverride(value),
        getOrganizationOverride: () => mockGetOrganizationOverride(),
    };
});

// Unused value, but required mock for test running
vi.mock("../network/Apis", () => {
    return {
        updateApiSessions: () => mockUpdateApiSessions(),
    };
});

const badAccessToken = mockAccessToken();
const goodAccessToken = mockAccessToken({
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

describe("helper functions", () => {
    describe("membershipsFromToken", () => {
        test("can handle token with undefined claims", () => {
            const state = membershipsFromToken();
            expect(state).toEqual(null);
        });
        test("can handle token with empty claims", () => {
            const state = membershipsFromToken({
                sub: "",
                organization: [],
            });
            expect(state).toEqual(null);
        });
        test("returns processed membership", () => {
            const state = membershipsFromToken({
                sub: "",
                organization: ["DHmy-organization"],
            });
            expect(state).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
        });

        test("returns active as first element of processed memberships", () => {
            const state = membershipsFromToken({
                sub: "",
                organization: ["DHmy-organization", "DHmy-other-organization"],
            });
            expect(state).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
        });
        // This is to protect our admins from messy Okta claims!
        test("return admin membership if present, even when not first", () => {
            const state = membershipsFromToken({
                sub: "",
                organization: ["DHmy-organization", "DHPrimeAdmins"],
            });
            expect(state).toEqual({
                parsedName: "PrimeAdmins",
                memberType: MemberType.PRIME_ADMIN,
            });
        });
    });
});
