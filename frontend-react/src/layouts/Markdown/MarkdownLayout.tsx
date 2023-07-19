import { Grid, GridContainer } from "@trussworks/react-uswds";
import { MDXProvider } from "@mdx-js/react";
import { Helmet } from "react-helmet-async";
import React from "react";

import { USSmartLink } from "../../components/USLink";
import MDXModules from "../../MDXModules";

export interface MarkdownLayoutProps {
    frontmatter?: {
        sidenav?: string;
        breadcrumbs?: Array<{ label: string; href: string }>;
        title?: string;
    };

    default: React.ComponentType;
    main?: JSX.Element;
    nav?: JSX.Element;
    mdx?: React.ComponentProps<typeof MDXProvider>;
}

/**
 * Props formatted to accept object spread directly from a "import * as XXX"
 * import (default as the component function, and any frontmatter properties under
 * frontmatter).
 *
 * FUTURE_TODO: Remove GridContainer once implemented in higher-level component.
 *
 * Default markdown layout.
 * @example
 * With sidenav:
 * +------+-----------------+
 * |      |                 |
 * |      |                 |
 * | SIDE |      MAIN       |
 * |      |                 |
 * |      |                 |
 * +------+-----------------+
 * Without sidenav:
 * +------------------------+
 * |                        |
 * |                        |
 * |         MAIN           |
 * |                        |
 * |                        |
 * +------------------------+
 */
export function MarkdownLayout({
    default: Component,
    main,
    nav,
    mdx,
    frontmatter: { title, sidenav } = {},
}: MarkdownLayoutProps) {
    const helmet = title ? (
        <Helmet>
            <title>{title}</title>
        </Helmet>
    ) : null;
    const LazyNav = sidenav ? React.lazy(MDXModules[`./${sidenav}.mdx`]) : null;

    return (
        <>
            {helmet}
            <MDXProvider
                components={{
                    a: USSmartLink,
                }}
                {...mdx}
            >
                <GridContainer className="usa-prose">
                    <Grid row className="flex-justify flex-align-start">
                        {nav == null && LazyNav != null ? (
                            <nav
                                aria-label="side-navigation"
                                className="tablet:grid-col-3 position-sticky top-0"
                            >
                                <React.Suspense fallback={<>...</>}>
                                    <LazyNav />
                                </React.Suspense>
                            </nav>
                        ) : (
                            nav
                        )}
                        {main ?? (
                            <main
                                className={
                                    LazyNav
                                        ? "tablet:grid-col-8"
                                        : "tablet:grid-col-12"
                                }
                            >
                                <Component />
                            </main>
                        )}
                    </Grid>
                </GridContainer>
            </MDXProvider>
        </>
    );
}

export default MarkdownLayout;

/**
 * Creates react-router-compatible lazy function for provided file path.
 */
export function lazyRouteMarkdown(path: string) {
    return async () => {
        const module = await MDXModules[`./${path}.mdx`]();
        return {
            Component() {
                return <MarkdownLayout {...module} />;
            },
        };
    };
}
