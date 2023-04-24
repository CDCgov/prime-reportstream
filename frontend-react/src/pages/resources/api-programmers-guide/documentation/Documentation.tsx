import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/api-programmers-guide/documentation/Documentation.mdx";

export interface DocumentationPageProps {}

export function DocumentationPage() {
    return (
        <MarkdownLayout sidenav={<></>}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default DocumentationPage;
