import { MarkdownDirectory } from "./MarkdownDirectory";

describe("MarkdownDirectory", () => {
    const testDir = new MarkdownDirectory({
        title: "Test Dir",
        slug: "/test-dir",
        root: "/test/dir",
        files: ["testFile.md"],
    });

    test("renders with params", () => {
        expect(testDir.title).toEqual("Test Dir");
        expect(testDir.slug).toEqual("/test-dir");
        expect(testDir.root).toEqual("/test/dir");
        expect(testDir.files).toEqual(["testFile.md"]);
    });

    test("getUrl", () => {
        expect(testDir.getUrl("testFile.md")).toEqual("/test/dir/testFile.md");
        expect(testDir.getUrl("noFile.md")).toEqual(undefined);
    });
});
