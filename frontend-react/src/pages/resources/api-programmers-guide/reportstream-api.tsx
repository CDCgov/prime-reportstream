import { MarkdownLayout } from "../../../components/Content/MarkdownLayout";
import ReportStreamAPIMarkdown from "../../../content/resources/api-programmers-guide/reportstream-api.mdx";
import Sidenav from "../../../content/resources/api-programmers-guide/sidenav.mdx";

export function ReportStreamAPIPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <ReportStreamAPIMarkdown />
        </MarkdownLayout>
    );
}
