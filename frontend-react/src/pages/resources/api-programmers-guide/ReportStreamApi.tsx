import { MarkdownLayout } from "../../../components/Content/MarkdownLayout";
import ReportStreamAPIMarkdown from "../../../content/resources/api-programmers-guide/ReportStreamApi.mdx";
import Sidenav from "../../../content/resources/api-programmers-guide/Sidenav.mdx";

export function ReportStreamAPIPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <ReportStreamAPIMarkdown />
        </MarkdownLayout>
    );
}
