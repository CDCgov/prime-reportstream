import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/api-programmers-guide/documentation/ResponsesFromReportStream.mdx";

export interface ResponsesFromReportStreamPageProps {}

export function ResponsesFromReportStreamPage() {
    return (
        <MarkdownLayout sidenav={<></>}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default ResponsesFromReportStreamPage;
