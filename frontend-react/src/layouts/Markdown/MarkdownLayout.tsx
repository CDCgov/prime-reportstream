import {
    Breadcrumb,
    BreadcrumbBar,
    Grid,
    GridContainer,
} from "@trussworks/react-uswds";
import { MDXProvider } from "@mdx-js/react";
import { Helmet } from "react-helmet-async";
import React from "react";
import * as reactUSWDS from "@trussworks/react-uswds";

import { USCrumbLink, USSmartLink, USNavLink } from "../../components/USLink";
import * as shared from "../../shared";
import MDXModules from "../../MDXModules";

/**
 * React components are functions that are pascal-cased so filtering is done
 * so.
 */
function filterComponents<T extends {}>(
    obj: T,
    include: Array<string & keyof T> = []
) {
    return Object.fromEntries(
        Object.entries(obj).filter(
            ([k, v]) =>
                include.includes(k as string & keyof T) ||
                (typeof v === "function" && k[0] === k[0].toUpperCase())
        )
    );
}

const uswdsComponents = filterComponents(reactUSWDS);
const sharedComponents = filterComponents(shared);

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
 * Markdown-formatted elements use lowercase variants from this list. If you need manual
 * control over props, use pascal-cased variants. react-uswds components dynamically added
 * to list (overridden by project shared components).
 */
const MDXComponents = {
    a: USSmartLink,
    A: USSmartLink,
    ...uswdsComponents,
    ...sharedComponents,
    Link: USSmartLink,
    USNavLink,
};

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
    frontmatter: { title, sidenav, breadcrumbs } = {},
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
            <GridContainer className="usa-prose">
                <Grid row className="flex-justify flex-align-start">
                    {nav == null && LazyNav != null ? (
                        <nav
                            aria-label="side-navigation"
                            className="tablet:grid-col-3 position-sticky top-0"
                        >
                            <React.Suspense fallback={<>...</>}>
                                <MDXProvider
                                    components={MDXComponents}
                                    {...mdx}
                                >
                                    <LazyNav />
                                </MDXProvider>
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
                            {breadcrumbs != null ? (
                                <BreadcrumbBar>
                                    {breadcrumbs.map((b) => (
                                        <Breadcrumb key={b.label}>
                                            {b.href ? (
                                                <USCrumbLink href={b.href}>
                                                    {b.label}
                                                </USCrumbLink>
                                            ) : (
                                                b.label
                                            )}
                                        </Breadcrumb>
                                    ))}
                                </BreadcrumbBar>
                            ) : null}
                            <MDXProvider components={MDXComponents} {...mdx}>
                                <Component />
                            </MDXProvider>
                        </main>
                    )}
                </Grid>
            </GridContainer>
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
