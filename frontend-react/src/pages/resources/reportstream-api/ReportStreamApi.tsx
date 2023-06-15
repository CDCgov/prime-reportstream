import { MarkdownLayout } from "../../../components/Content/MarkdownLayout";
import ReportStreamAPIMarkdown from "../../../content/resources/reportstream-api/ReportStreamApi.mdx";
import Sidenav from "../../../content/resources/reportstream-api/Sidenav.mdx";

export function ReportStreamAPIPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <ReportStreamAPIMarkdown />
        </MarkdownLayout>
    );
}
