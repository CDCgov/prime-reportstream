import { MarkdownDirectory } from "../../components/Markdown/MarkdownDirectory";
import markdownPagesGuide from "../../content/internal-user-guides/make-markdown-pages.md";
import StaticPagesFromDirectories from "../../components/Markdown/StaticPagesFromDirectories";

export const InternalUserGuidesDirectory = [
    new MarkdownDirectory("Create markdown pages", "create-markdown-pages", [
        markdownPagesGuide,
    ]),
];

const InternalUserGuides = () => {
    return (
        <StaticPagesFromDirectories directories={InternalUserGuidesDirectory} />
    );
};

export default InternalUserGuides;
