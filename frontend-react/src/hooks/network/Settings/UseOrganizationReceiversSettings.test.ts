import { renderHook, waitFor } from "@testing-library/react";

import { AppWrapper } from "../../../utils/CustomRenderUtils";
import {
    dummyReceivers,
    settingsServer,
} from "../../../__mocks__/SettingsMockServer";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";

import { useOrganizationReceiversSettings } from "./UseOrganizationReceiversSettings";

describe("useOrganizationReceiversSettings", () => {
    beforeAll(() => {
        settingsServer.listen();
    });
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());
    test("returns undefined if no active membership parsed name", () => {
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
        });
        const { result } = renderHook(
            () => useOrganizationReceiversSettings(),
            {
                wrapper: AppWrapper(),
            }
        );
        expect(result.current.data).toEqual(undefined);
        expect(result.current.isLoading).toEqual(true);
    });
    test("returns correct organization receivers", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
                service: "testReceiver",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: true,
            isUserSender: false,
        });
        const { result } = renderHook(
            () => useOrganizationReceiversSettings(),
            {
                wrapper: AppWrapper(),
            }
        );
        await waitFor(() =>
            expect(result.current.data).toEqual(dummyReceivers)
        );
        expect(result.current.isLoading).toEqual(false);
    });
});
