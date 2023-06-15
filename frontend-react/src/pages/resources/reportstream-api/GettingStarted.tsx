import React from "react";

import { MarkdownLayout } from "../../../components/Content/MarkdownLayout";
import GettingStarted from "../../../content/resources/reportstream-api/getting-started/GettingStarted.mdx";
import Sidenav from "../../../content/resources/reportstream-api/Sidenav.mdx";

export interface GettingStartedPageProps {}

export function GettingStartedPage() {
    return (
        <MarkdownLayout sidenav={<Sidenav />}>
            <GettingStarted />
        </MarkdownLayout>
    );
}
