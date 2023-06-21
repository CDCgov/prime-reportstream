import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/reportstream-api/documentation/ResponsesFromReportStream.mdx";
import Sidenav from "../../../../content/resources/reportstream-api/Sidenav.mdx";

export interface ResponsesFromReportStreamPageProps {}

export function ResponsesFromReportStreamPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default ResponsesFromReportStreamPage;
