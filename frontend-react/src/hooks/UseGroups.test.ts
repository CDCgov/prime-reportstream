import { act, renderHook } from "@testing-library/react-hooks";
import * as OktaReact from "@okta/okta-react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import {
    MembershipActionType,
    MemberType,
    useGroups,
} from "./UseGroups";

const mockAuth = jest.spyOn(OktaReact, "useOktaAuth");

describe("useGroups", () => {
    test("renders with default values", () => {
        mockAuth.mockReturnValue({} as IOktaContext);
        const { result } = renderHook(() => useGroups());
        expect(result.current.state.memberships).toBeUndefined();
        expect(result.current.state.active).toBeUndefined();
    });

    // test("accounts for non-standard groups", () => {
    //     mockAuth.mockReturnValue({
    //         authState: {
    //             accessToken: {
    //                 claims: {
    //                     //@ts-ignore
    //                     organization: ["NotYourStandardGroup"],
    //                 },
    //             },
    //         },
    //     });
    //     const { result } = renderHook(() => useGroups());
    //     expect(result.current.state.active?.memberType).toEqual("non-standard");
    // });

    test("can be set with AccessToken", () => {
        mockAuth.mockReturnValue({
            authState: {
                accessToken: {
                    claims: {
                        //@ts-ignore
                        organization: [
                            "DHPrimeAdmins",
                            "DHSender_ignore",
                            "DHmd_phd",
                        ],
                    },
                },
            },
        });
        const { result } = renderHook(() => useGroups());
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
                    },
                ],
                [
                    "DHSender_ignore",
                    {
                        parsedName: "ignore",
                        memberType: MemberType.SENDER,
                    },
                ],
                [
                    "DHmd_phd",
                    {
                        parsedName: "md-phd",
                        memberType: MemberType.RECEIVER,
                    },
                ],
            ])
        );
    });

    test("can swap active membership", () => {
        mockAuth.mockReturnValue({
            authState: {
                accessToken: {
                    claims: {
                        //@ts-ignore
                        organization: [
                            "DHPrimeAdmins",
                            "DHSender_ignore",
                            "DHmd_phd",
                        ],
                    },
                },
            },
        });
        const { result } = renderHook(() => useGroups());
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

    // test("can override as admin", () => {
    //     mockAuth.mockReturnValue({
    //         authState: {
    //             accessToken: {
    //                 claims: {
    //                     //@ts-ignore
    //                     organization: ["DHPrimeAdmins"],
    //                 },
    //             },
    //         },
    //     });
    //     const { result } = renderHook(() => useGroups());
    //     expect(result.current.state.active).toEqual({
    //         parsedName: "PrimeAdmins",
    //         memberType: MemberType.PRIME_ADMIN,
    //     });
    //     act(() =>
    //         result.current.dispatch({
    //             type: MembershipActionType.ADMIN_OVERRIDE,
    //             payload: {
    //                 parsedName: "sender-org",
    //                 memberType: MemberType.SENDER,
    //             },
    //         })
    //     );
    //     expect(result.current.state.active).toEqual({
    //         parsedName: "sender-org",
    //         memberType: MemberType.SENDER,
    //     });
    // });
});

// describe("membershipsFromToken extra coverage", () => {
//     test("can handle undefined token", () => {
//         const state = membershipsFromToken({} as AccessToken);
//         expect(state).toEqual({
//             active: undefined,
//             memberships: undefined,
//         });
//     });
// });

describe("membershipReducer extra coverage", () => {
    test("can handle bad request", () => {
        mockAuth.mockReturnValue({
            authState: {
                accessToken: {
                    claims: {
                        //@ts-ignore
                        organization: ["DHPrimeAdmins"],
                    },
                },
            },
        });
        const { result } = renderHook(() => useGroups());

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
