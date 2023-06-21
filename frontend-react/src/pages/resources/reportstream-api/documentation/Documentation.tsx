import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/reportstream-api/documentation/Documentation.mdx";
import Sidenav from "../../../../content/resources/reportstream-api/Sidenav.mdx";

export interface DocumentationPageProps {}

export function DocumentationPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default DocumentationPage;
