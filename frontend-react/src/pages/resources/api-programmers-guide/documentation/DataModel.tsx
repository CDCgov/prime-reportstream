import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/api-programmers-guide/documentation/data-model/DataModel.mdx";

export interface DataModelPageProps {}

export function DataModelPage() {
    return (
        <MarkdownLayout sidenav={<></>}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default DataModelPage;
