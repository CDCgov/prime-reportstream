import { Grid, GridContainer } from "@trussworks/react-uswds";
import { MDXProvider } from "@mdx-js/react";
import { Helmet } from "react-helmet-async";
import React, { useMemo, useState } from "react";
import * as reactUSWDS from "@trussworks/react-uswds";
import type { TocEntry } from "remark-mdx-toc";

import { USSmartLink, USNavLink } from "../../components/USLink";
import * as shared from "../../shared";
import PageHeader from "../../shared/PageHeader/PageHeader";

import { TableOfContents } from "./TableOfContents";
import MarkdownLayoutContext from "./Context";
import { LayoutSidenav, LayoutMain } from "./LayoutComponents";
import styles from "./MarkdownLayout.module.scss";

/**
 * React components are functions that are pascal-cased so filtering is done
 * so.
 */
function filterComponents<T extends {}>(
    obj: T,
    include: Array<string & keyof T> = [],
) {
    return Object.fromEntries(
        Object.entries(obj).filter(
            ([k, v]) =>
                include.includes(k as string & keyof T) ||
                (typeof v === "function" && k.startsWith(k[0].toUpperCase())),
        ),
    );
}

const uswdsComponents = filterComponents(reactUSWDS);
const sharedComponents = filterComponents(shared);

export interface MarkdownLayoutProps {
    children: React.ReactNode;
    frontmatter?: {
        sidenav?: string;
        breadcrumbs?: Array<{ label: string; href: string }>;
        title?: string;
        subtitle?: string | string[];
        callToAction?: Array<{ label: string; href: string }>;
        lastUpdated?: string;
        toc?: boolean | { depth?: number };
        backToTop?: boolean;
        isPrimaryDarker?: boolean;
    };
    toc?: TocEntry[];
    main?: React.ReactNode;
    nav?: React.ReactNode;
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
 * Provides a LayoutSidenav component in mdx context that when used will automatically
 * enable the sidenav of layout.
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
    children,
    main,
    mdx,
    frontmatter: {
        title,
        breadcrumbs,
        subtitle,
        callToAction,
        lastUpdated,
        toc,
        backToTop,
        isPrimaryDarker,
    } = {},
    toc: tocEntries,
}: MarkdownLayoutProps) {
    const [sidenavContent, setSidenavContent] =
        useState<React.ReactNode>(undefined);
    const [mainContent, setMainContent] = useState<React.ReactNode>(undefined);
    const ctx = useMemo(() => {
        return {
            sidenavContent,
            setSidenavContent,
            mainContent,
            setMainContent,
        };
    }, [mainContent, sidenavContent]);
    const tocObj = toc ? (typeof toc === "object" ? toc : {}) : null;
    const subtitleArr = Array.isArray(subtitle)
        ? subtitle
        : subtitle
        ? [subtitle]
        : [];
    const isHeader = Boolean(
        title || breadcrumbs || callToAction || lastUpdated || toc,
    );

    return (
        <MarkdownLayoutContext.Provider value={ctx}>
            {title && (
                <Helmet>
                    <title>{title}</title>
                </Helmet>
            )}
            <GridContainer className="usa-prose">
                <Grid row className="flex-justify flex-align-start">
                    {sidenavContent ? (
                        <nav
                            aria-label="side-navigation"
                            className={`${styles.sidenav} tablet:grid-col-3`}
                        >
                            <MDXProvider
                                {...mdx}
                                components={{
                                    ...MDXComponents,
                                    ...mdx?.components,
                                }}
                            >
                                {sidenavContent}
                            </MDXProvider>
                        </nav>
                    ) : null}
                    {main ?? (
                        <main
                            className={
                                sidenavContent
                                    ? "tablet:grid-col-8"
                                    : "tablet:grid-col-12"
                            }
                        >
                            {isHeader && (
                                <PageHeader
                                    title={title}
                                    breadcrumbs={breadcrumbs}
                                    subtitleArr={subtitleArr}
                                    callToAction={callToAction}
                                    isPrimaryDarker={isPrimaryDarker}
                                    lastUpdated={lastUpdated}
                                />
                            )}
                            {tocObj && tocEntries && (
                                <>
                                    <b>On this page:</b>
                                    <TableOfContents
                                        {...tocObj}
                                        items={tocEntries}
                                    />
                                    <hr />
                                </>
                            )}
                            <MDXProvider
                                {...mdx}
                                components={{
                                    ...MDXComponents,
                                    LayoutSidenav,
                                    LayoutMain,
                                    ...mdx?.components,
                                }}
                            >
                                {mainContent ?? children}
                            </MDXProvider>
                            {backToTop && (
                                <p>
                                    <USSmartLink href="#top">
                                        Back to top
                                    </USSmartLink>
                                </p>
                            )}
                        </main>
                    )}
                </Grid>
            </GridContainer>
        </MarkdownLayoutContext.Provider>
    );
}

export default MarkdownLayout;
