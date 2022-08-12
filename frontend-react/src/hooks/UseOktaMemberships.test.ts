import { AuthState } from "@okta/okta-auth-js";
import { act, renderHook } from "@testing-library/react-hooks";

import { mockToken } from "../utils/TestUtils";

import {
    MemberType,
    MembershipActionType,
    membershipsFromToken,
    useOktaMemberships,
} from "./UseOktaMemberships";

const fakeAuthStateForOrgs = (orgs: string[]): Partial<AuthState> => ({
    isAuthenticated: true,
    accessToken: mockToken({
        claims: {
            sub: "",
            organization: orgs,
        },
    }),
});

describe("useOktaMemberships", () => {
    test("renders with default values", () => {
        const { result } = renderHook(() => useOktaMemberships(null));
        expect(result.current.state.memberships).toBeUndefined();
        expect(result.current.state.active).toBeUndefined();
    });

    test("accounts for non-standard groups", () => {
        // const fakeToken = {
        //     claims: {
        //         //@ts-ignore
        //         organization: ["NotYourStandardGroup"],
        //         sub: "", // necessary to pass type check
        //     },
        // };
        const fakeAuthState = fakeAuthStateForOrgs(["NotYourStandardGroup"]);
        const { result } = renderHook(() => useOktaMemberships(fakeAuthState));
        expect(result.current.state.active?.memberType).toEqual("non-standard");
    });

    test("can be set with AccessToken", () => {
        // const fakeToken = {
        //     claims: {
        //         //@ts-ignore
        //         organization: ["DHPrimeAdmins", "DHSender_ignore", "DHmd_phd"],
        //         sub: "", // necessary to pass type check
        //     },
        // };
        const fakeAuthState = fakeAuthStateForOrgs([
            "DHPrimeAdmins",
            "DHSender_ignore",
            "DHmd_phd",
        ]);
        const { result } = renderHook(() => useOktaMemberships(fakeAuthState));
        expect(result.current.state.active).toEqual({
            parsedName: "PrimeAdmins",
            memberType: MemberType.PRIME_ADMIN,
        });
        expect(result.current.state.memberships).toEqual(
            new Map([
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
            ])
        );
    });

    test("can override as admin", () => {
        // const fakeToken = {
        //     claims: {
        //         //@ts-ignore
        //         organization: ["DHPrimeAdmins"],
        //         sub: "",
        //     },
        // };
        const fakeAuthState = fakeAuthStateForOrgs(["DHPrimeAdmins"]);
        const { result } = renderHook(() => useOktaMemberships(fakeAuthState));
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
});

describe("membershipsFromToken extra coverage", () => {
    test("can handle undefined token", () => {
        const state = membershipsFromToken(mockToken());
        expect(state).toEqual({
            active: undefined,
            memberships: undefined,
        });
    });
});
