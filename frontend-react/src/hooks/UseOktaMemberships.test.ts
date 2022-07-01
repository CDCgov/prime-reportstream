import { act, renderHook } from "@testing-library/react-hooks";

import { mockToken } from "../utils/TestUtils";

import {
    MemberType,
    MembershipActionType,
    membershipsFromToken,
    useOktaMemberships,
} from "./UseOktaMemberships";

describe("useOktaMemberships", () => {
    test("renders with default values", () => {
        const { result } = renderHook(() => useOktaMemberships(undefined));
        expect(result.current.state.memberships).toBeUndefined();
        expect(result.current.state.active).toBeUndefined();
    });

    test("accounts for non-standard groups", () => {
        const fakeToken = {
            claims: {
                //@ts-ignore
                organization: ["NotYourStandardGroup"],
                sub: "", // necessary to pass type check
            },
        };
        const token = mockToken(fakeToken);
        const { result } = renderHook(() => useOktaMemberships(token));
        expect(result.current.state.active?.memberType).toEqual("non-standard");
    });

    test("can be set with AccessToken", () => {
        const fakeToken = {
            claims: {
                //@ts-ignore
                organization: ["DHPrimeAdmins", "DHSender_ignore", "DHmd_phd"],
                sub: "", // necessary to pass type check
            },
        };

        const { result } = renderHook(() =>
            useOktaMemberships(mockToken(fakeToken))
        );
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

    test("can swap active membership", () => {
        const fakeToken = {
            claims: {
                //@ts-ignore
                organization: ["DHPrimeAdmins", "DHSender_ignore", "DHmd_phd"],
                sub: "",
            },
        };
        const { result } = renderHook(() =>
            useOktaMemberships(mockToken(fakeToken))
        );
        expect(result.current.state.active).toEqual({
            parsedName: "PrimeAdmins",
            memberType: MemberType.PRIME_ADMIN,
        });
        act(() =>
            result.current.dispatch({
                type: MembershipActionType.SWITCH,
                payload: "DHmd_phd",
            })
        );
        expect(result.current.state.active).toEqual({
            parsedName: "md-phd",
            memberType: MemberType.RECEIVER,
        });
    });

    test("can override as admin", () => {
        const fakeToken = {
            claims: {
                //@ts-ignore
                organization: ["DHPrimeAdmins"],
                sub: "",
            },
        };
        const { result } = renderHook(() =>
            useOktaMemberships(mockToken(fakeToken))
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

describe("membershipReducer extra coverage", () => {
    test("can handle bad request", () => {
        const fakeToken = {
            claims: {
                //@ts-ignore
                organization: ["DHPrimeAdmins"],
                sub: "",
            },
        };
        const { result } = renderHook(() =>
            useOktaMemberships(mockToken(fakeToken))
        );

        // bad switch
        act(() =>
            result.current.dispatch({
                type: MembershipActionType.SWITCH,
                payload: "org-does-not-exist",
            })
        );
        expect(result.current.state.active?.parsedName).toEqual("PrimeAdmins");
    });
});
