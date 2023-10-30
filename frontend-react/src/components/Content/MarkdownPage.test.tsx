import path from "path";

import testMdUrl from "../../content/markdown-test.md?url";
import testMd from "../../content/markdown-test.md?raw";
import { renderApp } from "../../utils/CustomRenderUtils";

import { MarkdownDirectory } from "./MarkdownDirectory";
import MarkdownPage from "./MarkdownPage";

const __dirname = new URL(".", import.meta.url).toString();

describe("DirectoryAsPage", () => {
    vi.spyOn(global, "fetch").mockImplementation(() =>
        Promise.resolve(new Response(testMd, { status: 200 })),
    );
    const testDir = new MarkdownDirectory()
        .setTitle("Test Dir")
        .setSlug("test-dir")
        .addFile(path.join(__dirname, testMdUrl));
    test("Renders without error", () => {
        renderApp(<MarkdownPage directory={testDir} />);
    });
});
