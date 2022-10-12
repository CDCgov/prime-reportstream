import { act, renderHook } from "@testing-library/react-hooks";

import {
    watersServer,
    WatersTestHeaderValue,
} from "../../__mocks__/WatersMockServer";
import { QueryWrapper } from "../../utils/CustomRenderUtils";
import { ContentType } from "../UseFileHandler";

import { useWatersUploader } from "./WatersHooks";

const mockCallbackFn = jest.fn();

describe("useWatersUploader", () => {
    beforeAll(() => watersServer.listen());
    afterEach(() => watersServer.resetHandlers());
    afterAll(() => watersServer.close());
    const renderHookWithQueryWrapper = () =>
        renderHook(() => useWatersUploader(mockCallbackFn), {
            wrapper: QueryWrapper(),
        });
    test("has default state", () => {
        const { result } = renderHookWithQueryWrapper();
        expect(result.current.isWorking).toEqual(false);
        expect(result.current.sendFile).toBeInstanceOf(Function);
        expect(result.current.uploaderError).toBeNull();
    });
    test("posts to /api/waters", async () => {
        const { result, waitForNextUpdate } = renderHookWithQueryWrapper();
        let response;
        await act(async () => {
            const post = result.current.sendFile({
                contentType: ContentType.CSV,
                fileContent: "",
                fileName: "",
                // test response trigger
                client: WatersTestHeaderValue.TEST_NAME,
            });
            await waitForNextUpdate();
            expect(result.current.isWorking).toEqual(true);
            response = await post;
        });
        expect(response).toEqual({
            endpoint: "upload",
        });
    });
    test("posts to /api/validate when validateOnly param is true", async () => {
        const { result, waitForNextUpdate } = renderHook(
            () => useWatersUploader(mockCallbackFn, true),
            {
                wrapper: QueryWrapper(),
            }
        );
        let response;
        await act(async () => {
            const post = result.current.sendFile({
                contentType: ContentType.CSV,
                fileContent: "",
                fileName: "",
                // test response trigger
                client: WatersTestHeaderValue.TEST_NAME,
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
