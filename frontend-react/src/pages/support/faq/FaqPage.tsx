import { MarkdownLayout } from "../../../layouts/Markdown/MarkdownLayout";
import FaqIndex, {
    frontmatter,
    toc,
} from "../../../content/support/faq/FaqIndex.mdx";
import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import { FeatureName } from "../../../AppRouter";

export function FaqPage() {
    const crumbProps: CrumbsProps = {
        crumbList: [
            { label: FeatureName.SUPPORT, path: "/support" },
            { label: "Frequently asked questions" },
        ],
    };

    return (
        <MarkdownLayout frontmatter={frontmatter} toc={toc}>
            <Crumbs {...crumbProps}></Crumbs>
            <FaqIndex />
        </MarkdownLayout>
    );
}
