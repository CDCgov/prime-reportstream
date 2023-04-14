import { renderHook } from "@testing-library/react";

import { orgServer } from "../../../../__mocks__/OrganizationMockServer";
import { AppWrapper } from "../../../../utils/CustomRenderUtils";

import useCreateOrganizationPublicKey from "./UseCreateOrganizationPublicKey";

describe("useCreateOrganizationPublicKey", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    const renderWithAppWrapper = () =>
        renderHook(() => useCreateOrganizationPublicKey(), {
            wrapper: AppWrapper(),
        });

    test("has default state", async () => {
        const { result } = renderWithAppWrapper();
        expect(result.current.isLoading).toEqual(false);
        expect(result.current.isError).toEqual(false);
    });

    //TODO: test network call
});
