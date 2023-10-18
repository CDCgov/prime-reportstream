import { renderHook, waitFor } from "@testing-library/react";

import { dataDashboardServer } from "../../../__mocks__/DataDashboardMockServer";
import { mockSessionContentReturnValue } from "../../../contexts/__mocks__/SessionContext";
import { AppWrapper } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../UseOktaMemberships";

import useReceiverSubmitters from "./UseReceiverSubmitters";

describe("useReceiverSubmitters", () => {
    beforeAll(() => dataDashboardServer.listen());
    afterEach(() => dataDashboardServer.resetHandlers());
    afterAll(() => dataDashboardServer.close());

    describe("with no Organization name", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: undefined,

                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: false,
                environment: "test",
            });
        });

        test("returns undefined", async () => {
            const { result } = renderHook(() => useReceiverSubmitters(), {
                wrapper: AppWrapper(),
            });
            await waitFor(() => expect(result.current.data).toEqual(undefined));
            expect(result.current.isLoading).toEqual(true);
        });
    });

    describe("with Organization and service name", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrg",
                    service: "testService",
                },

                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                environment: "test",
            });
        });

        test("returns receiver meta and submitters", async () => {
            const { result } = renderHook(
                () => useReceiverSubmitters("testService"),
                {
                    wrapper: AppWrapper(),
                },
            );

            await waitFor(() => expect(result.current.data).toHaveLength(1));
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
