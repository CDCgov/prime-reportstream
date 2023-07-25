import { act, renderHook } from "@testing-library/react";

import {
    watersServer,
    WatersTestHeaderValue,
} from "../../__mocks__/WatersMockServer";
import { AppWrapper } from "../../utils/CustomRenderUtils";
import { STANDARD_SCHEMA_OPTIONS } from "../../senders/hooks/UseSenderSchemaOptions";
import { ContentType, FileType } from "../../utils/TemporarySettingsAPITypes";

import { useWatersUploader } from "./WatersHooks";

const mockCallbackFn = jest.fn();

describe("useWatersUploader", () => {
    beforeAll(() => watersServer.listen());
    afterEach(() => watersServer.resetHandlers());
    afterAll(() => watersServer.close());
    const renderHookWithAppWrapper = () =>
        renderHook(() => useWatersUploader(mockCallbackFn), {
            wrapper: AppWrapper(),
        });
    test("has default state", () => {
        const { result } = renderHookWithAppWrapper();
        expect(result.current.isWorking).toEqual(false);
        expect(result.current.sendFile).toBeInstanceOf(Function);
        expect(result.current.uploaderError).toBeNull();
    });
    test("posts to /api/validate when validateOnly param is true", async () => {
        const { result } = renderHook(() => useWatersUploader(mockCallbackFn), {
            wrapper: AppWrapper(),
        });
        let response;
        await act(async () => {
            const post = result.current.sendFile({
                contentType: ContentType.CSV,
                fileContent: "",
                fileName: "",
                // test response trigger
                client: WatersTestHeaderValue.TEST_NAME,
                format: FileType.CSV,
                schema: STANDARD_SCHEMA_OPTIONS[0].value,
            });
            response = await post;
        });
        expect(response).toEqual({
            endpoint: "validate",
        });
    });
});
