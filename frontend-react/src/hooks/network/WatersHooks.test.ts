import { act, renderHook } from "@testing-library/react-hooks";

import { watersServer } from "../../__mocks__/WatersMockServer";
import { QueryWrapper } from "../../utils/CustomRenderUtils";

import { useWatersUploader, useWatersValidator } from "./WatersHooks";

describe("useWatersUploader", () => {
    beforeAll(() => watersServer.listen());
    afterEach(() => watersServer.resetHandlers());
    afterAll(() => watersServer.close());

    test("has default state", () => {
        const { result } = renderHook(() => useWatersUploader(), {
            wrapper: QueryWrapper(),
        });
        expect(result.current.isWorking).toEqual(false);
        expect(result.current.sendFile).toBeInstanceOf(Function);
    });
    test("posts to /api/waters", async () => {
        const { result, waitForNextUpdate } = renderHook(
            () => useWatersUploader(),
            {
                wrapper: QueryWrapper(),
            }
        );
        let response;
        await act(async () => {
            const post = result.current.sendFile({
                contentType: "",
                fileContent: "",
                fileName: "",
                // test response trigger
                client: "test-endpoint-name",
            });
            await waitForNextUpdate();
            expect(result.current.isWorking).toEqual(true);
            response = await post;
        });
        expect(response).toEqual({
            endpoint: "upload",
        });
    });
});

describe("useWatersValidator", () => {
    beforeAll(() => watersServer.listen());
    afterEach(() => watersServer.resetHandlers());
    afterAll(() => watersServer.close());

    test("posts to /api/validate", async () => {
        const { result, waitForNextUpdate } = renderHook(
            () => useWatersValidator(),
            {
                wrapper: QueryWrapper(),
            }
        );
        let response;
        await act(async () => {
            const post = result.current.sendFile({
                contentType: "",
                fileContent: "",
                fileName: "",
                // test response trigger
                client: "test-endpoint-name",
            });
            await waitForNextUpdate();
            expect(result.current.isWorking).toEqual(true);
            response = await post;
        });
        expect(response).toEqual({
            endpoint: "validate",
        });
    });
});
