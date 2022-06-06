import { MarkdownDirectory } from "../../components/Markdown/MarkdownDirectory";
import markdownPagesGuide from "../../content/internal-user-guides/make-markdown-pages.md";
import StaticPageFromDirectories from "../../components/Markdown/StaticPageFromDirectories";

export const InternalUserGuidesDirectory = [
    new MarkdownDirectory("Create markdown pages", "create-markdown-pages", [
        markdownPagesGuide,
    ]),
];

const InternalUserGuides = () => {
    return (
        <StaticPageFromDirectories directories={InternalUserGuidesDirectory} />
    );
};

export default InternalUserGuides;
