import { MarkdownDirectory } from "./MarkdownDirectory";
import MarkdownPage from "./MarkdownPage";
import testMd from "../../content/markdown-test.md?url";
import { renderApp, screen } from "../../utils/CustomRenderUtils";

describe("DirectoryAsPage", () => {
    const testDir = new MarkdownDirectory()
        .setTitle("Test Dir")
        .setSlug("test-dir")
        .addFile(testMd);
    test("Renders without error", () => {
        renderApp(
            <MarkdownPage directory={testDir} data-testid="markdownpage" />,
        );
        expect(screen.getByTestId("markdownpage")).toBeInTheDocument();
    });
});
