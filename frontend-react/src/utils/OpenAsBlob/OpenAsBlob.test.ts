import openAsBlob from "./OpenAsBlob";

describe("OpenAsBlob", () => {
    const objectURL = "fake-url";
    const data = "test";
    const blob = new Blob([data], { type: "text/plain" });

    beforeEach(() => {
        vi.stubGlobal(
            "Blob",
            vi.fn(() => blob),
        );
        vi.stubGlobal("URL", { ...URL, createObjectURL: vi.fn(() => objectURL), revokeObjectURL: vi.fn(() => void 0) });
        vi.spyOn(window, "open").mockImplementation(() => null);
    });
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    test("opens blob data url in new window", () => {
        const mockBlob = vi.mocked(Blob);
        const mockURL = vi.mocked(URL);
        const mockWindow = vi.mocked(window);
        openAsBlob(data);

        expect(mockBlob).toBeCalledWith([data], { type: "text/plain" });
        expect(mockURL.createObjectURL).toBeCalledWith(blob);
        expect(mockURL.revokeObjectURL).toBeCalledWith(objectURL);
        expect(mockWindow.open).toBeCalledWith(objectURL, "_blank");
    });
});
