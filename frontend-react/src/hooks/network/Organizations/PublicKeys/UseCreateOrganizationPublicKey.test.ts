import { renderHook } from "@testing-library/react";

import {
    dummyPublicKey,
    orgServer,
} from "../../../../__mocks__/OrganizationMockServer";
import { AppWrapper } from "../../../../utils/CustomRenderUtils";
import { mockSessionContext } from "../../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../../UseOktaMemberships";

import useCreateOrganizationPublicKey from "./UseCreateOrganizationPublicKey";

describe("useCreateOrganizationPublicKey", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    beforeEach(() => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testOrgPublicKey",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
        });
    });

    const renderWithAppWrapper = () =>
        renderHook(() => useCreateOrganizationPublicKey(), {
            wrapper: AppWrapper(),
        });

    test("posts to /public-keys API and returns response", async () => {
        const { result } = renderWithAppWrapper();
        expect(result.current.isLoading).toEqual(false);
        expect(result.current.isError).toEqual(false);

        const mutateAsyncResult = await result.current.mutateAsync({
            kid: "testOrg.elr-0",
            sender: "elr-0",
        });
        expect(mutateAsyncResult).toEqual(dummyPublicKey);
    });
});
