import { renderHook } from "@testing-library/react-hooks";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MembershipController, MemberType } from "../../UseOktaMemberships";
import { SessionController } from "../../UseSessionStorage";
import { historyServer } from "../../../__mocks__/HistoryMockServer";

import { useReportsList } from "./ReportsHooks";

describe("ReportsHooks", () => {
    beforeAll(() => historyServer.listen());
    afterEach(() => historyServer.resetHandlers());
    afterAll(() => historyServer.close());
    test("useReportsList", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrg",
                        senderName: undefined,
                    },
                },
            } as MembershipController,
            store: {} as SessionController, // TS yells about removing this because of types
        });
        const { result, waitForNextUpdate } = renderHook(() =>
            useReportsList()
        );
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.loading).toBeFalsy();
        expect(result.current.data).toHaveLength(3);
    });
});
