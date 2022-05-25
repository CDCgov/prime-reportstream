import { act, renderHook } from "@testing-library/react-hooks";
import * as OktaAuth from "@okta/okta-react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import { MembershipActionType, MemberType, useGroups } from "./UseGroups";

const mockAuth = jest.spyOn(OktaAuth, "useOktaAuth");

describe("useGroups", () => {
    test("renders with default values", () => {
        mockAuth.mockReturnValue({} as IOktaContext);
        const { result } = renderHook(() => useGroups());
        expect(result.current.state.memberships).toBeUndefined();
        expect(result.current.state.active).toBeUndefined();
    });

    test("renders with parsed Okta groups", () => {
        //@ts-ignore
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
});
