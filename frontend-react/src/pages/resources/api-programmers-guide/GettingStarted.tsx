import React from "react";

import { MarkdownLayout } from "../../../components/Content/MarkdownLayout";
import GettingStarted from "../../../content/resources/api-programmers-guide/getting-started/GettingStarted.mdx";

export interface GettingStartedPageProps {}

export function GettingStartedPage() {
    return (
        <MarkdownLayout sidenav={<></>}>
            <GettingStarted />
        </MarkdownLayout>
    );
}
