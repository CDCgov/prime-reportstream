import React from "react";

import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";
import Markdown from "../../../../content/resources/api-programmers-guide/documentation/DataModel.mdx";
//import Sidenav from "../../../../content/resources/api-programmers-guide/Sidenav.mdx";

export interface DataModelPageProps {}

export function DataModelPage() {
    return (
        <MarkdownLayout>
            <Markdown />
        </MarkdownLayout>
    );
}
