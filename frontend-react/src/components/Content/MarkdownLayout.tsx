import { GridContainer } from "@trussworks/react-uswds";
import { MDXProvider } from "@mdx-js/react";

import { USSmartLink } from "../USLink";
import { GridRow } from "../Grid";

export interface MarkdownLayoutProps {
    sidenav?: JSX.Element;
    children?: React.ReactNode;
}

export function MarkdownLayout({ sidenav, children }: MarkdownLayoutProps) {
    return (
        <MDXProvider
            components={{
                a: USSmartLink,
            }}
        >
            <GridContainer>
                <GridRow>
                    {sidenav ? (
                        <nav
                            aria-label="side-navigation"
                            className="grid-col-auto"
                        >
                            {sidenav}
                        </nav>
                    ) : undefined}
                    <main className="grid-col">{children}</main>
                </GridRow>
            </GridContainer>
        </MDXProvider>
    );
}
