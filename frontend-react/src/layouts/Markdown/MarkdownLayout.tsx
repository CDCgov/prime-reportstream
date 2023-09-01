import { MDXProvider } from "@mdx-js/react";
import { Helmet } from "react-helmet-async";
import React, { useMemo, useState } from "react";
import * as reactUSWDS from "@trussworks/react-uswds";
import type { TocEntry } from "remark-mdx-toc";
import { useMatches } from "react-router";
import classNames from "classnames";

import { USSmartLink, USNavLink } from "../../components/USLink";
import * as shared from "../../shared";

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
    frontmatter?: Frontmatter;
    toc?: TocEntry[];
    article?: React.ReactNode;
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
    article,
    mdx,
    frontmatter: {
        title,
        breadcrumbs,
        subtitle,
        callToAction,
        lastUpdated,
        toc,
        backToTop,
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
    const matches = useMatches() as RsRouteObject[];
    const { handle = {} } = matches.at(-1) ?? {};
    const { isFullWidth } = handle;

    return (
        <MarkdownLayoutContext.Provider value={ctx}>
            {title && (
                <Helmet>
                    <title>{title}</title>
                </Helmet>
            )}
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
            {article ?? (
                <article
                    id="main-content"
                    className={classNames(
                        "usa-prose",
                        sidenavContent
                            ? "tablet:grid-col-9"
                            : "tablet:grid-col-12",
                    )}
                >
                    {isHeader &&
                        (isFullWidth ? (
                            <shared.HeroWrapper isAlternate>
                                <shared.PageHeader
                                    title={title}
                                    breadcrumbs={breadcrumbs}
                                    subtitleArr={subtitleArr}
                                    callToAction={callToAction}
                                    lastUpdated={lastUpdated}
                                    className="usa-section usa-prose grid-container"
                                />
                            </shared.HeroWrapper>
                        ) : (
                            <shared.PageHeader
                                title={title}
                                breadcrumbs={breadcrumbs}
                                subtitleArr={subtitleArr}
                                callToAction={callToAction}
                                lastUpdated={lastUpdated}
                                className="usa-prose"
                            />
                        ))}
                    {tocObj && tocEntries && (
                        <>
                            <b>On this page:</b>
                            <TableOfContents {...tocObj} items={tocEntries} />
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
                        <USSmartLink id="back-to-top" href="#top">
                            Back to top
                        </USSmartLink>
                    )}
                </article>
            )}
        </MarkdownLayoutContext.Provider>
    );
}

export default MarkdownLayout;
