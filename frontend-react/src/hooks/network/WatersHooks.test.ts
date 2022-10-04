import { renderHook } from "@testing-library/react-hooks";

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
    test("Upload set as default", () => {});
    test("Validate set with validateOnly hook parameter", () => {});
});
