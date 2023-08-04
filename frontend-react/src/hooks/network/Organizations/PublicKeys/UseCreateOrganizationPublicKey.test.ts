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

    const renderWithAppWrapper = () =>
        renderHook(() => useCreateOrganizationPublicKey(), {
            wrapper: AppWrapper(),
        });

    describe("when authorized, 200", () => {
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
                environment: "test",
            });
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

    describe("when unauthorized, 401", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "",
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
                environment: "test",
            });
        });

        test("does not post to /public-keys API and throws 401", async () => {
            const { result } = renderWithAppWrapper();
            expect(result.current.isLoading).toEqual(false);
            expect(result.current.isError).toEqual(false);

            await expect(
                async () =>
                    await result.current.mutateAsync({
                        kid: "testOrg.elr-0",
                        sender: "elr-0",
                    }),
            ).rejects.toThrowError("Request failed with status code 401");
        });
    });
});
