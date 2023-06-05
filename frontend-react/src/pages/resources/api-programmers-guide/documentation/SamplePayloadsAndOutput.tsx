import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/api-programmers-guide/documentation/SamplePayloadsAndOutput.mdx";
import Sidenav from "../../../../content/resources/api-programmers-guide/Sidenav.mdx";

export interface SamplePayloadsAndOutputPageProps {}

export function SamplePayloadsAndOutputPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default SamplePayloadsAndOutputPage;
