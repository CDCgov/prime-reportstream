import path from "path";

import testMdUrl from "../../content/markdown-test.md?url";
import testMd from "../../content/markdown-test.md?raw";

import { MarkdownDirectory } from "./MarkdownDirectory";
import MarkdownPage from "./MarkdownPage";

const __dirname = new URL(".", import.meta.url).toString();

describe("DirectoryAsPage", () => {
    const testDir = new MarkdownDirectory()
        .setTitle("Test Dir")
        .setSlug("test-dir")
        .addFile(path.join(__dirname, testMdUrl));
    test("Renders without error", () => {
        vi.spyOn(global, "fetch").mockImplementation(async () =>
            Promise.resolve(new Response(testMd, { status: 200 })),
        );
        render(<MarkdownPage directory={testDir} />);
    });
});
