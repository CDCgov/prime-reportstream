import { waitFor } from "@testing-library/react";

import { dataDashboardServer } from "../../../__mocks__/DataDashboardMockServer";
import { renderHook } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";

import useReceiverSubmitters from "./UseReceiverSubmitters";

describe("useReceiverSubmitters", () => {
    beforeAll(() => dataDashboardServer.listen());
    afterEach(() => dataDashboardServer.resetHandlers());
    afterAll(() => dataDashboardServer.close());

    describe("with no Organization name", () => {
        test("returns undefined", async () => {
            const { result } = renderHook(() => useReceiverSubmitters(), {
                providers: {
                    Session: {
                        authState: {
                            accessToken: { accessToken: "TOKEN" },
                        } as any,
                        activeMembership: undefined,

                        user: {
                            isUserAdmin: false,
                            isUserReceiver: false,
                            isUserSender: false,
                            isUserTransceiver: false,
                        } as any,
                    },
                },
            });
            await waitFor(() => expect(result.current.data).toEqual(undefined));
            expect(result.current.isLoading).toEqual(false);
        });
    });

    describe("with Organization and service name", () => {
        test("returns receiver meta and submitters", async () => {
            const { result } = renderHook(
                () => useReceiverSubmitters("testService"),
                {
                    providers: {
                        Session: {
                            authState: {
                                accessToken: { accessToken: "TOKEN" },
                            } as any,
                            activeMembership: {
                                memberType: MemberType.RECEIVER,
                                parsedName: "testOrg",
                                service: "testService",
                            },

                            user: {
                                isUserAdmin: false,
                                isUserReceiver: true,
                                isUserSender: false,
                                isUserTransceiver: false,
                            } as any,
                        },
                    },
                },
            );

            await waitFor(() => expect(result.current.data).toHaveLength(1));
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
