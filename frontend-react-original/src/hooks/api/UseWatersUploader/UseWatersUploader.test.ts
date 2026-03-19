import { act } from "@testing-library/react";

import useWatersUploader from "./UseWatersUploader";
import {
    watersServer,
    WatersTestHeaderValue,
} from "../../../__mockServers__/WatersMockServer";
import { renderHook } from "../../../utils/CustomRenderUtils";
import {
    ContentType,
    FileType,
} from "../../../utils/TemporarySettingsAPITypes";
import { STANDARD_SCHEMA_OPTIONS } from "../../UseSenderSchemaOptions/UseSenderSchemaOptions";

const mockCallbackFn = vi.fn();

describe("useWatersUploader", () => {
    beforeAll(() => watersServer.listen());
    afterEach(() => watersServer.resetHandlers());
    afterAll(() => watersServer.close());
    const renderHookWithAppWrapper = () =>
        renderHook(() => useWatersUploader(mockCallbackFn));
    test("has default state", () => {
        const { result } = renderHookWithAppWrapper();
        expect(result.current.isPending).toEqual(false);
        expect(result.current.mutateAsync).toBeInstanceOf(Function);
        expect(result.current.error).toBeNull();
    });
    test("posts to /api/validate when validateOnly param is true", async () => {
        const { result } = renderHook(() => useWatersUploader(mockCallbackFn));
        let response;
        await act(async () => {
            const post = result.current.mutateAsync({
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
