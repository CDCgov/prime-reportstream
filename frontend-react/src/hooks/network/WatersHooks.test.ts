import { act, renderHook } from "@testing-library/react-hooks";

import { watersServer } from "../../__mocks__/WatersMockServer";
import { QueryWrapper } from "../../utils/CustomRenderUtils";

import { useWatersUploader } from "./WatersHooks";

describe("Waters API React Query Hooks", () => {
    beforeAll(() => watersServer.listen());
    afterEach(() => watersServer.resetHandlers());
    afterAll(() => watersServer.close());

    test("Values on render", () => {
        const { result } = renderHook(() => useWatersUploader(), {
            wrapper: QueryWrapper(),
        });
        expect(result.current.isWorking).toEqual(false);
        expect(result.current.sendFile).toBeInstanceOf(Function);
    });
    test("Upload set as default", async () => {
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
    test("Validate set with validateOnly hook parameter", async () => {
        const { result, waitForNextUpdate } = renderHook(
            () => useWatersUploader(true),
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
