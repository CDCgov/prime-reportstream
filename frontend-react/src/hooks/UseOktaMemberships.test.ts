import { MemberType, membershipsFromToken } from "./UseOktaMemberships";

let mockGetSessionMembershipState = jest.fn();
let mockGetOrganizationOverride = jest.fn();

const mockStoreSessionMembershipState = jest.fn();
const mockStoreOrganizationOverride = jest.fn();
const mockUpdateApiSessions = jest.fn();

jest.mock("../utils/SessionStorageTools", () => {
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
jest.mock("../network/Apis", () => {
    return {
        updateApiSessions: () => mockUpdateApiSessions(),
    };
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
