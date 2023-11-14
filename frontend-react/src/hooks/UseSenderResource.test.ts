import { waitFor } from "@testing-library/react";

import { renderHook } from "../utils/CustomRenderUtils";
import { dummySender, orgServer } from "../__mocks__/OrganizationMockServer";
import { MemberType, MembershipSettings } from "../utils/OrganizationUtils";

import useSenderResource from "./UseSenderResource";

describe("useSenderResource", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no sender available on membership", () => {
        const { result } = renderHook(() => useSenderResource(), {
            providers: {
                Session: {
                    authState: {
                        accessToken: { accessToken: "TOKEN" },
                    },
                    activeMembership: {
                        memberType: MemberType.NON_STAND,
                        service: undefined,
                    } as MembershipSettings,
                    user: {
                        isUserSender: true,
                    },
                },
            },
        });
        expect(result.current.data).toEqual(undefined);
    });
    test("returns correct sender match", async () => {
        const { result } = renderHook(() => useSenderResource(), {
            providers: {
                Session: {
                    authState: {
                        accessToken: { accessToken: "TOKEN" },
                    },
                    activeMembership: {
                        memberType: MemberType.SENDER,
                        parsedName: "testOrg",
                        service: "testSender",
                    },
                    user: {
                        isUserSender: true,
                    },
                },
            },
        });
        await waitFor(() => expect(result.current.data).toEqual(dummySender));
        expect(result.current.isLoading).toEqual(false);
    });
});
