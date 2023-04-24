import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/api-programmers-guide/documentation/SamplePayloadsAndOutput.mdx";

export interface SamplePayloadsAndOutputPageProps {}

export function SamplePayloadsAndOutputPage() {
    return (
        <MarkdownLayout sidenav={<></>}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default SamplePayloadsAndOutputPage;
