import testMd from "../../content/markdown-test.md?url";
import { renderApp } from "../../utils/CustomRenderUtils";

import { MarkdownDirectory } from "./MarkdownDirectory";
import MarkdownPage from "./MarkdownPage";

describe("DirectoryAsPage", () => {
    const testDir = new MarkdownDirectory()
        .setTitle("Test Dir")
        .setSlug("test-dir")
        .addFile(testMd);
    test("Renders without error", () => {
        renderApp(<MarkdownPage directory={testDir} />);
    });
});
