import { act, renderHook, RenderResult } from "@testing-library/react-hooks";

import {
    MembershipActionType,
    MembershipController,
    MemberType,
    useGroups,
} from "./UseGroups";

const mimicLoginWithGroups = (
    result: RenderResult<MembershipController>,
    groups: string[]
) =>
    act(() =>
        result.current.dispatch({
            type: MembershipActionType.UPDATE,
            payload: {
                claims: {
                    //@ts-ignore
                    organization: groups,
                },
            },
        })
    );

describe("useGroups", () => {
    test("renders with default values", () => {
        const { result } = renderHook(() => useGroups());
        expect(result.current.state.memberships).toBeUndefined();
        expect(result.current.state.active).toBeUndefined();
    });

    test("can be set with AccessToken", () => {
        const { result } = renderHook(() => useGroups());
        mimicLoginWithGroups(result, [
            "DHPrimeAdmins",
            "DHSender_ignore",
            "DHmd_phd",
        ]);
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
        const { result } = renderHook(() => useGroups());
        mimicLoginWithGroups(result, [
            "DHPrimeAdmins",
            "DHSender_ignore",
            "DHmd_phd",
        ]);
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
