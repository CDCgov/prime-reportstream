import { renderHook } from "@testing-library/react-hooks";

import { QueryWrapper } from "../../../utils/CustomRenderUtils";
import {
    dummyReceivers,
    orgServer,
} from "../../../config/__mocks__/OrganizationMockServer";
import { MemberType } from "../../UseOktaMemberships";
import { mockAuthReturnValue } from "../__mocks__/OktaAuth";

import { useOrganizationReceiversSettings } from "./UseOrganizationReceiversSettings";

describe("useOrganizationReceivers", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no active membership parsed name", () => {
        mockAuthReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: undefined,
            dispatch: () => {},
            initialized: true,
        });
        const { result } = renderHook(
            () => useOrganizationReceiversSettings(),
            {
                wrapper: QueryWrapper(),
            }
        );
        expect(result.current.data).toEqual(undefined);
        expect(result.current.isLoading).toEqual(true);
    });
    test("returns correct organization receivers", async () => {
        mockAuthReturnValue({
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
        });
        const { result, waitForNextUpdate } = renderHook(
            () => useOrganizationReceiversSettings(),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual(dummyReceivers);
        expect(result.current.isLoading).toEqual(false);
    });
});
