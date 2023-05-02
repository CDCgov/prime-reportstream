import { MarkdownLayout } from "../../../components/Content/MarkdownLayout";
import ReportStreamAPIMarkdown from "../../../content/resources/api-programmers-guide/ReportStreamApi.mdx";

export function ReportStreamAPIPage() {
    return (
        <MarkdownLayout sidenav={<></>}>
            <ReportStreamAPIMarkdown />
        </MarkdownLayout>
    );
}
