import { AccessToken, AuthState } from "@okta/okta-auth-js";
import { act, renderHook } from "@testing-library/react";

import { mockToken } from "../utils/TestUtils";

import {
    MemberType,
    MembershipActionType,
    membershipsFromToken,
    useOktaMemberships,
    calculateMembershipsWithOverride,
    membershipReducer,
} from "./UseOktaMemberships";

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

// do we need to mock the whole state, or just the token?
const fakeAuthStateForOrgs = (orgs: string[]): Partial<AuthState> => ({
    isAuthenticated: true,
    accessToken: mockToken({
        claims: {
            sub: "",
            organization: orgs,
        },
    }),
});

const renderWithAuthUpdates = (initialProps: AuthState | null) =>
    renderHook((authState: AuthState | null) => useOktaMemberships(authState), {
        initialProps,
    });

describe("useOktaMemberships", () => {
    beforeEach(() => {
        mockGetSessionMembershipState = jest.fn(() => ({}));
    });

    describe("initialization", () => {
        test("returns default initial values", () => {
            const { result } = renderHook(() => useOktaMemberships(null));
            expect(result.current.state.activeMembership).toBeUndefined();
        });

        test("initializes with stored state where available", async () => {
            mockGetSessionMembershipState = jest.fn(() => ({
                activeMembership: {
                    parsedName: "MyOrganization",
                    memberType: MemberType.NON_STAND,
                    service: undefined,
                },
            }));
            const { result } = renderHook(() => useOktaMemberships(null));
            expect(result.current.state.activeMembership).toEqual({
                parsedName: "MyOrganization",
                memberType: MemberType.NON_STAND,
                service: undefined,
            });
        });

        test("initializes with stored state and override where available", async () => {
            mockGetSessionMembershipState = jest.fn(() => ({
                activeMembership: {
                    parsedName: "MyFirstOrg",
                    memberType: MemberType.RECEIVER,
                    service: undefined,
                },
            }));
            mockGetOrganizationOverride = jest.fn(() => ({
                parsedName: "MyOverrideOrg",
                memberType: MemberType.SENDER,
                service: undefined,
            }));
            const { result } = renderHook(() => useOktaMemberships(null));
            expect(result.current.state.activeMembership).toEqual({
                parsedName: "MyOverrideOrg",
                memberType: MemberType.SENDER,
                service: undefined,
            });
        });
        test("accounts for non-standard groups", () => {
            // Should result in parsing as non-standard due to lack of DH prefix
            const fakeAuthState = fakeAuthStateForOrgs([
                "NotYourStandardGroup",
            ]);
            const { result } = renderHook(() =>
                useOktaMemberships(fakeAuthState),
            );
            expect(result.current.state.activeMembership?.memberType).toEqual(
                "non-standard",
            );
        });
    });

    describe("on dispatch", () => {
        test("can set membership from AccessToken", () => {
            const fakeAuthState = fakeAuthStateForOrgs(["DHmy-organization"]);
            const { result } = renderHook(() => useOktaMemberships(null));
            expect(result.current.state.activeMembership).toBeUndefined();
            // simulate login
            act(() => {
                result.current.dispatch({
                    type: MembershipActionType.SET_MEMBERSHIPS_FROM_TOKEN,
                    payload: fakeAuthState.accessToken as AccessToken,
                });
            });
            expect(result.current.state.activeMembership).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
        });

        test("can override active membership (Admin-only behavior)", () => {
            const newActive = {
                parsedName: "sender-org",
                memberType: MemberType.SENDER,
            };
            const fakeAuthState = fakeAuthStateForOrgs(["DHmy-organization"]);
            const { result } = renderHook(() =>
                useOktaMemberships(fakeAuthState),
            );
            expect(result.current.state.activeMembership).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
            act(() =>
                result.current.dispatch({
                    type: MembershipActionType.ADMIN_OVERRIDE,
                    payload: newActive,
                }),
            );
            expect(result.current.state.activeMembership).toEqual(newActive);
            expect(mockStoreOrganizationOverride).toHaveBeenCalledWith(
                JSON.stringify(newActive),
            );
        });

        test("can be reset", () => {
            const fakeAuthState = fakeAuthStateForOrgs(["DHmy-organization"]);
            const { result } = renderHook(() =>
                useOktaMemberships(fakeAuthState),
            );
            expect(result.current.state.activeMembership).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
            expect(result.current.state.initialized).toEqual(true);

            act(() =>
                result.current.dispatch({
                    type: MembershipActionType.RESET,
                }),
            );
            expect(result.current.state).toEqual({
                memberships: undefined,
                activeMembership: null,
                service: undefined,
                initialized: true,
            });
        });
    });

    describe("reactive behavior", () => {
        test("dispatches `SET_MEMBERSHIP_FROM_TOKEN` when token memberships change", async () => {
            const fakeAuthState = fakeAuthStateForOrgs(["DHmy-organization"]);
            const { result, rerender } = renderWithAuthUpdates(null);
            expect(result.current.state.activeMembership).toBeUndefined();
            rerender(fakeAuthState);
            expect(result.current.state.activeMembership).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
        });
        test("dispatches `RESET` when authState is not authenticated", async () => {
            const fakeAuthState = fakeAuthStateForOrgs(["DHmy-organization"]);
            const { result, rerender } = renderWithAuthUpdates(fakeAuthState);
            expect(result.current.state.activeMembership).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
            expect(result.current.state.initialized).toEqual(true);

            rerender({
                isAuthenticated: false,
            });
            expect(result.current.state.activeMembership).toEqual(null);
            expect(result.current.state.initialized).toEqual(true);
        });
    });
});

describe("helper functions", () => {
    describe("membershipsFromToken", () => {
        test("can handle token with undefined claims", () => {
            const state = membershipsFromToken(mockToken());
            expect(state).toEqual({
                activeMembership: null,
            });
        });
        test("can handle token with empty claims", () => {
            const state = membershipsFromToken(
                mockToken({
                    claims: {
                        sub: "",
                        organization: [],
                    },
                }),
            );
            expect(state).toEqual({
                activeMembership: null,
            });
        });
        test("returns processed membership", () => {
            const state = membershipsFromToken(
                mockToken({
                    claims: {
                        sub: "",
                        organization: ["DHmy-organization"],
                    },
                }),
            );
            expect(state).toEqual({
                activeMembership: {
                    parsedName: "my-organization",
                    memberType: MemberType.RECEIVER,
                },
            });
        });

        test("returns active as first element of processed memberships", () => {
            const state = membershipsFromToken(
                mockToken({
                    claims: {
                        sub: "",
                        organization: [
                            "DHmy-organization",
                            "DHmy-other-organization",
                        ],
                    },
                }),
            );
            expect(state).toEqual({
                activeMembership: {
                    parsedName: "my-organization",
                    memberType: MemberType.RECEIVER,
                },
            });
        });
        // This is to protect our admins from messy Okta claims!
        test("return admin membership if present, even when not first", () => {
            const state = membershipsFromToken(
                mockToken({
                    claims: {
                        sub: "",
                        organization: ["DHmy-organization", "DHPrimeAdmins"],
                    },
                }),
            );
            expect(state).toEqual({
                activeMembership: {
                    parsedName: "PrimeAdmins",
                    memberType: MemberType.PRIME_ADMIN,
                },
            });
        });
    });
    describe("calculateMembershipsWithOverride", () => {
        test("assigns stored override to active", () => {
            mockGetOrganizationOverride = jest.fn(() => ({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            }));
            const overriddenState = calculateMembershipsWithOverride({
                activeMembership: {
                    parsedName: "my-overridden-org",
                    memberType: MemberType.NON_STAND,
                },
            });
            expect(mockGetOrganizationOverride).toHaveBeenCalledTimes(1);
            expect(overriddenState.activeMembership).toEqual({
                parsedName: "my-organization",
                memberType: MemberType.RECEIVER,
            });
        });
        test("returns passed state if there is no override present", () => {
            mockGetOrganizationOverride = jest.fn(() => {});
            const initialState = {
                activeMembership: {
                    parsedName: "my-organization",
                    memberType: MemberType.RECEIVER,
                },
            };
            const overridenState =
                calculateMembershipsWithOverride(initialState);
            expect(mockGetOrganizationOverride).toHaveBeenCalledTimes(1);
            expect(overridenState).toEqual(initialState);
        });
    });
    describe("membershipReducer", () => {
        test("stores state string in session on each invocation", () => {
            membershipReducer(
                {
                    activeMembership: {
                        parsedName: "my-organization",
                        memberType: MemberType.RECEIVER,
                    },
                },
                {
                    type: MembershipActionType.SET_MEMBERSHIPS_FROM_TOKEN,
                    payload: mockToken({
                        claims: {
                            sub: "",
                            organization: ["DHmy-organization"],
                        },
                    }),
                },
            );
            expect(mockStoreSessionMembershipState).toHaveBeenCalledWith(
                JSON.stringify({
                    activeMembership: {
                        parsedName: "my-organization",
                        memberType: MemberType.RECEIVER,
                    },
                    initialized: true,
                }),
            );
            membershipReducer(
                {
                    activeMembership: {
                        parsedName: "my-organization",
                        memberType: MemberType.RECEIVER,
                    },
                },
                {
                    type: MembershipActionType.RESET,
                },
            );
            expect(mockStoreSessionMembershipState).toHaveBeenCalledWith(
                JSON.stringify({
                    activeMembership: null,
                }),
            );
        });
    });
});
