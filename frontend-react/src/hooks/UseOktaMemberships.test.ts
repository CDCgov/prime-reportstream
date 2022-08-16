import { AccessToken, AuthState } from "@okta/okta-auth-js";
import { act, renderHook } from "@testing-library/react-hooks";

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

const fakeMemberships = new Map([
    [
        "DHPrimeAdmins",
        {
            parsedName: "PrimeAdmins",
            memberType: MemberType.PRIME_ADMIN,
            senderName: undefined,
        },
    ],
    [
        "DHSender_ignore",
        {
            parsedName: "ignore",
            memberType: MemberType.SENDER,
            senderName: "default",
        },
    ],
    [
        "DHmd_phd",
        {
            parsedName: "md-phd",
            memberType: MemberType.RECEIVER,
            senderName: undefined,
        },
    ],
]);

jest.mock("../contexts/SessionStorageTools", () => {
    return {
        storeSessionMembershipState: (value: string) =>
            mockStoreSessionMembershipState(value),
        getSessionMembershipState: () => mockGetSessionMembershipState(),
        storeOrganizationOverride: (value: string) =>
            mockStoreOrganizationOverride(value),
        getOrganizationOverride: () => mockGetOrganizationOverride(),
    };
});

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
            expect(result.current.state.memberships).toBeUndefined();
            expect(result.current.state.active).toBeUndefined();
        });

        test("initializes with stored state where available", async () => {
            mockGetSessionMembershipState = jest.fn(() => ({
                memberships: fakeMemberships,
                active: fakeMemberships.get("DHmd_phd"),
            }));

            const { result } = renderHook(() => useOktaMemberships(null));

            expect(result.current.state.active).toEqual(
                fakeMemberships.get("DHmd_phd")
            );

            // this works in the test (since the session functionality is mocked) but actually won't work in the app
            // this is a bug due to the fact taht Maps cannot be serialized
            expect(result.current.state.memberships).toEqual(fakeMemberships);
        });

        test("initializes with stored state and override where available", async () => {
            mockGetSessionMembershipState = jest.fn(() => ({
                memberships: fakeMemberships,
                active: fakeMemberships.get("DHmd_phd"),
            }));

            mockGetOrganizationOverride = jest.fn(() =>
                fakeMemberships.get("DHSender_ignore")
            );

            const { result } = renderHook(() => useOktaMemberships(null));

            expect(result.current.state.active).toEqual(
                fakeMemberships.get("DHSender_ignore")
            );

            // this works in the test (since the session functionality is mocked) but actually won't work in the app
            // this is a bug due to the fact taht Maps cannot be serialized
            expect(result.current.state.memberships).toEqual(fakeMemberships);
        });
        test("accounts for non-standard groups", () => {
            const fakeAuthState = fakeAuthStateForOrgs([
                "NotYourStandardGroup",
            ]);
            const { result } = renderHook(() =>
                useOktaMemberships(fakeAuthState)
            );

            expect(result.current.state.active?.memberType).toEqual(
                "non-standard"
            );
        });
    });

    describe("on dispatch", () => {
        test("can be set with AccessToken", () => {
            const fakeAuthState = fakeAuthStateForOrgs([
                "DHPrimeAdmins",
                "DHSender_ignore",
                "DHmd_phd",
            ]);
            const { result } = renderHook(() => useOktaMemberships(null));

            expect(result.current.state.active).toBeUndefined();
            expect(result.current.state.memberships).toEqual(undefined);

            // simulate login
            act(() => {
                result.current.dispatch({
                    type: MembershipActionType.SET_MEMBERSHIPS_FROM_TOKEN,
                    payload: fakeAuthState.accessToken as AccessToken,
                });
            });

            expect(result.current.state.active).toEqual({
                parsedName: "PrimeAdmins",
                memberType: MemberType.PRIME_ADMIN,
            });
            expect(result.current.state.memberships).toEqual(fakeMemberships);
        });

        test("can be overriden as admin", () => {
            const fakeAuthState = fakeAuthStateForOrgs(["DHPrimeAdmins"]);
            const { result } = renderHook(() =>
                useOktaMemberships(fakeAuthState)
            );

            expect(result.current.state.active).toEqual({
                parsedName: "PrimeAdmins",
                memberType: MemberType.PRIME_ADMIN,
            });
            act(() =>
                result.current.dispatch({
                    type: MembershipActionType.ADMIN_OVERRIDE,
                    payload: {
                        parsedName: "sender-org",
                        memberType: MemberType.SENDER,
                    },
                })
            );
            expect(result.current.state.active).toEqual({
                parsedName: "sender-org",
                memberType: MemberType.SENDER,
            });
        });

        test("can be reset", () => {
            const fakeAuthState = fakeAuthStateForOrgs(["DHPrimeAdmins"]);
            const { result } = renderHook(() =>
                useOktaMemberships(fakeAuthState)
            );

            expect(result.current.state.active).toEqual({
                parsedName: "PrimeAdmins",
                memberType: MemberType.PRIME_ADMIN,
            });

            act(() =>
                result.current.dispatch({
                    type: MembershipActionType.RESET,
                })
            );
            expect(result.current.state).toEqual({
                memberships: undefined,
                active: undefined,
            });
        });
    });

    describe("reactive behavior", () => {
        test("dispatches `SET_MEMBERSHIP_FROM_TOKEN` when token memberships change", async () => {
            const fakeAuthState = fakeAuthStateForOrgs([
                "DHPrimeAdmins",
                "DHSender_ignore",
                "DHmd_phd",
            ]);

            const { result, rerender } = renderWithAuthUpdates(null);

            expect(result.current.state.active).toBeUndefined();
            expect(result.current.state.memberships).toEqual(undefined);

            rerender(fakeAuthState);

            expect(result.current.state.active).toEqual({
                parsedName: "PrimeAdmins",
                memberType: MemberType.PRIME_ADMIN,
            });
            expect(result.current.state.memberships).toEqual(fakeMemberships);
        });
        test("dispatches `RESET` when authState is not authenticated", async () => {
            const fakeAuthState = fakeAuthStateForOrgs([
                "DHPrimeAdmins",
                "DHSender_ignore",
                "DHmd_phd",
            ]);

            const { result, rerender } = renderWithAuthUpdates(fakeAuthState);

            expect(result.current.state.active).toEqual({
                parsedName: "PrimeAdmins",
                memberType: MemberType.PRIME_ADMIN,
            });
            expect(result.current.state.memberships).toEqual(fakeMemberships);

            rerender({
                isAuthenticated: false,
            });
            expect(result.current.state.active).toEqual(undefined);
            expect(result.current.state.memberships).toEqual(undefined);
        });
    });
});

describe("helper functions", () => {
    describe("membershipsFromToken", () => {
        test("can handle token with undefined claims", () => {
            const state = membershipsFromToken(mockToken());
            expect(state).toEqual({
                active: undefined,
                memberships: undefined,
            });
        });
        test("can handle token with empty claims", () => {
            const state = membershipsFromToken(
                mockToken({
                    claims: {
                        sub: "",
                        organization: [],
                    },
                })
            );
            expect(state).toEqual({
                active: undefined,
                memberships: undefined,
            });
        });
        test("returns processed memberships", () => {
            const state = membershipsFromToken(
                mockToken({
                    claims: {
                        sub: "",
                        organization: ["DHSender_ignore"],
                    },
                })
            );
            expect(state).toEqual({
                active: fakeMemberships.get("DHSender_ignore"),
                memberships: new Map([
                    ["DHSender_ignore", fakeMemberships.get("DHSender_ignore")],
                ]),
            });
        });

        test("returns active as first element of processed memberships", () => {
            const state = membershipsFromToken(
                mockToken({
                    claims: {
                        sub: "",
                        organization: [
                            "DHPrimeAdmins",
                            "DHSender_ignore",
                            "DHmd_phd",
                        ],
                    },
                })
            );
            expect(state).toEqual({
                active: fakeMemberships.get("DHPrimeAdmins"),
                memberships: fakeMemberships,
            });
        });
    });
    describe("calculateMembershipsWithOverride", () => {
        test("assigns stored override to active", () => {
            mockGetOrganizationOverride = jest.fn(() =>
                fakeMemberships.get("DHmd_phd")
            );
            const overridenState = calculateMembershipsWithOverride({
                memberships: fakeMemberships,
                active: fakeMemberships.get("PrimeAdmins"),
            });

            expect(mockGetOrganizationOverride).toHaveBeenCalledTimes(1);
            expect(overridenState.active).toEqual(
                fakeMemberships.get("DHmd_phd")
            );
        });
        test("returns passed state if there is no override present", () => {
            mockGetOrganizationOverride = jest.fn(() => {});
            const initialState = {
                memberships: fakeMemberships,
                active: fakeMemberships.get("DHPrimeAdmins"),
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
                    active: {
                        parsedName: "some_active_org",
                        memberType: MemberType.RECEIVER,
                    },
                    memberships: new Map(),
                },
                {
                    type: MembershipActionType.SET_MEMBERSHIPS_FROM_TOKEN,
                    payload: mockToken({
                        claims: {
                            sub: "",
                            organization: [
                                "DHPrimeAdmins",
                                "DHSender_ignore",
                                "DHmd_phd",
                            ],
                        },
                    }),
                }
            );
            expect(mockStoreSessionMembershipState).toHaveBeenCalledWith(
                JSON.stringify({
                    active: fakeMemberships.get("DHPrimeAdmins"),
                    memberships: fakeMemberships,
                })
            );

            membershipReducer(
                {
                    active: {
                        parsedName: "some_active_org",
                        memberType: MemberType.RECEIVER,
                    },
                    memberships: new Map(),
                },
                {
                    type: MembershipActionType.RESET,
                }
            );
            expect(mockStoreSessionMembershipState).toHaveBeenCalledWith(
                JSON.stringify({
                    active: undefined,
                    memberships: undefined,
                })
            );
        });
    });
});
