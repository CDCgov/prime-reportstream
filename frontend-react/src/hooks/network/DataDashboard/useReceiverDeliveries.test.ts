import { renderHook, waitFor } from "@testing-library/react";

import { dataDashboardServer } from "../../../__mocks__/DataDashboardMockServer";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { AppWrapper } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../UseOktaMemberships";

import useReceiverDeliveries from "./UseReceiverDeliveries";

describe("useReceiverDeliveries", () => {
    beforeAll(() => dataDashboardServer.listen());
    afterEach(() => dataDashboardServer.resetHandlers());
    afterAll(() => dataDashboardServer.close());

    describe("with no Organization name", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: undefined,
                dispatch: () => {},
                initialized: true,
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: false,
                environment: "test",
            });
        });

        test("returns undefined", async () => {
            const { result } = renderHook(() => useReceiverDeliveries(), {
                wrapper: AppWrapper(),
            });
            await waitFor(() => expect(result.current.data).toEqual(undefined));
            expect(result.current.isLoading).toEqual(true);
        });
    });

    describe("with Organization and service name", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrg",
                    service: "testService",
                },
                dispatch: () => {},
                initialized: true,
                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                environment: "test",
            });
        });

        test("returns receiver meta and deliveries", async () => {
            const { result } = renderHook(
                () => useReceiverDeliveries("testService"),
                {
                    wrapper: AppWrapper(),
                },
            );

            await waitFor(() => expect(result.current.data).toHaveLength(1));
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
