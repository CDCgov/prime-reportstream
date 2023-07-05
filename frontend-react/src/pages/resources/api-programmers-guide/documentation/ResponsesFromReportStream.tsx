import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/api-programmers-guide/documentation/ResponsesFromReportStream.mdx";
import Sidenav from "../../../../content/resources/api-programmers-guide/Sidenav.mdx";

export interface ResponsesFromReportStreamPageProps {}

export function ResponsesFromReportStreamPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default ResponsesFromReportStreamPage;
