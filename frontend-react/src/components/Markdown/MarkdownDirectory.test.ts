import { MarkdownDirectory } from "./MarkdownDirectory";

describe("MarkdownDirectory", () => {
    const testDir = new MarkdownDirectory("Test Dir", "/test-dir", []);

    test("renders with params", () => {
        expect(testDir.title).toEqual("Test Dir");
        expect(testDir.slug).toEqual("/test-dir");
        expect(testDir.files).toEqual([]);
    });
});
