import testMd from "../../content/markdown-test.md";
import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { MarkdownDirectory } from "./MarkdownDirectory";
import DirectoryAsPage from "./DirectoryAsPage";

describe("DirectoryAsPage", () => {
    const testDir = new MarkdownDirectory()
        .setTitle("Test Dir")
        .setSlug("test-dir")
        .addFile(testMd);
    test("Renders without error", () => {
        renderWithRouter(<DirectoryAsPage directory={testDir} />);
    });
});
