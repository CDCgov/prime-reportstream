import { act } from "@testing-library/react";

import useValueSetActivation from "./UseValueSetActivation";
import { lookupTableServer } from "../../../../__mockServers__/LookupTableMockServer";
import { AppWrapper, renderHook } from "../../../../utils/CustomRenderUtils";

describe("useValueSetActivation", () => {
    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    const renderWithAppWrapper = () =>
        renderHook(() => useValueSetActivation(), {
            wrapper: AppWrapper(),
        });

    test("returns trigger and loading indicator", () => {
        const { result } = renderWithAppWrapper();
        const { mutateAsync, isPending, error } = result.current;
        expect(isPending).toEqual(false);
        expect(mutateAsync).toBeInstanceOf(Function);
        expect(error).toBeNull();
    });

    test("mutation trigger returns expected values and tracks loading state", async () => {
        const { result } = renderWithAppWrapper();
        const { mutateAsync, isPending } = result.current;
        expect(isPending).toEqual(false);

        let activateResult;
        await act(async () => {
            const activationPromise = mutateAsync({
                tableVersion: 1,
                tableName: "any",
            });
            activateResult = await activationPromise;
        });

        expect(activateResult).toEqual({
            lookupTableVersionId: 2,
            tableName: "sender_automation_value_set_row",
            tableVersion: 2,
            isActive: true,
            createdBy: "again@example.com",
            createdAt: "later",
            tableSha256Checksum: "checksum",
        });
    });
});
