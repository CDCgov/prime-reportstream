import { renderHook } from "@testing-library/react-hooks";

import { orgServer, testSender } from "../__mocks__/OrganizationMockServer";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";

import useSenderMode from "./UseSenderMode";
import { MemberType } from "./UseOktaMemberships";

describe("useSenderMode", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("provides accurate sender mode", async () => {
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.SENDER,
                        parsedName: "ignore",
                    },
                },
            },
        });
        const { result, waitForNextUpdate } = renderHook(() =>
            useSenderMode("testOrg", "testSender")
        );
        await waitForNextUpdate();
        expect(result.current).toEqual(testSender.customerStatus);
    });
});
