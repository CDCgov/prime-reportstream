import { ElementDirectory, MarkdownDirectory } from "./MarkdownDirectory";

const TestDiv = <div>Hello, world</div>;
const TestElement = () => TestDiv;

describe("ContentDirectory extensions", () => {
    const markdown = new MarkdownDirectory(
        "Test Dir",
        "test-dir",
        [],
        "Test desc"
    );
    const element = new ElementDirectory(
        "Element Dir",
        "element-dir",
        "Elements in a directory",
        TestElement
    );
    test("Markdown constructor", () => {
        expect(markdown.title).toEqual("Test Dir");
        expect(markdown.slug).toEqual("test-dir");
        expect(markdown.files).toEqual([]);
        expect(markdown.desc).toEqual("Test desc");
    });
    test("Element constructor", () => {
        expect(element.title).toEqual("Element Dir");
        expect(element.slug).toEqual("element-dir");
        expect(element.desc).toEqual("Elements in a directory");
        expect(element.element()).toEqual(TestDiv);
    });
});
