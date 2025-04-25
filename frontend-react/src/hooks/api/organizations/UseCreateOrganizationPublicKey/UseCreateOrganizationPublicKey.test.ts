import useCreateOrganizationPublicKey from "./UseCreateOrganizationPublicKey";
import { dummyPublicKey, orgServer } from "../../../../__mockServers__/OrganizationMockServer";
import { renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

describe("useCreateOrganizationPublicKey", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    const renderWithAppWrapper = () => renderHook(() => useCreateOrganizationPublicKey());

    describe("when authorized, 200", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.SENDER,
                    parsedName: "testOrg",
                    service: "testOrgPublicKey",
                },

                user: {
                    isUserAdmin: false,
                    isUserReceiver: false,
                    isUserSender: true,
                    isUserTransceiver: false,
                } as any,
            });
        });

        test("posts to /public-keys API and returns response", async () => {
            const { result } = renderWithAppWrapper();
            expect(result.current.isPending).toEqual(false);
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
            mockSessionContentReturnValue({
                authState: {
                    accessToken: "",
                } as any,
                activeMembership: {
                    memberType: MemberType.SENDER,
                    parsedName: "testOrg",
                    service: "testOrgPublicKey",
                },

                user: {
                    isUserAdmin: false,
                    isUserReceiver: false,
                    isUserSender: true,
                    isUserTransceiver: false,
                } as any,
            });
        });

        test("does not post to /public-keys API and throws 401", async () => {
            const { result } = renderWithAppWrapper();
            expect(result.current.isPending).toEqual(false);
            expect(result.current.isError).toEqual(false);

            await expect(
                async () =>
                    await result.current.mutateAsync({
                        kid: "testOrg.elr-0",
                        sender: "elr-0",
                    }),
            ).rejects.toThrow("Request failed with status code 401");
        });
    });
});
