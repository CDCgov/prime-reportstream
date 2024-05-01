import { MDXProvider } from "@mdx-js/react";
import * as reactUSWDS from "@trussworks/react-uswds";
import classNames from "classnames";
import { ComponentProps, ReactNode, useMemo, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useMatches } from "react-router";
import type { TocEntry } from "remark-mdx-toc";

import MarkdownLayoutContext from "./Context";
import { LayoutBackToTop, LayoutMain, LayoutSidenav } from "./LayoutComponents";
import styles from "./MarkdownLayout.module.scss";
import { TableOfContents } from "./TableOfContents";
import { createMeta } from "./utils";
import { USNavLink, USSmartLink } from "../../components/USLink";
import useSessionContext from "../../contexts/Session/useSessionContext";
import * as shared from "../../shared";

/**
 * React components are functions that are pascal-cased so filtering is done
 * so.
 */
function filterComponents<T extends object>(
    obj: T,
    include: (string & keyof T)[] = [],
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
    children: ReactNode;
    frontmatter?: Frontmatter;
    toc?: TocEntry[];
    article?: ReactNode;
    nav?: ReactNode;
    mdx?: ComponentProps<typeof MDXProvider>;
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
function MarkdownLayout({
    children,
    article,
    mdx,
    frontmatter = {},
    toc: tocEntries,
}: MarkdownLayoutProps) {
    const {
        title,
        breadcrumbs,
        subtitle,
        callToAction,
        lastUpdated,
        toc,
        backToTop,
    } = frontmatter;
    const { config } = useSessionContext();
    const [sidenavContent, setSidenavContent] = useState<ReactNode>(undefined);
    const [mainContent, setMainContent] = useState<ReactNode>(undefined);
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
        title ?? breadcrumbs ?? callToAction ?? lastUpdated ?? toc,
    );
    const matches = useMatches() as RsRouteObject[];
    const { handle = {} } = matches.at(-1) ?? {};
    const { isFullWidth } = handle;
    const meta = useMemo(
        () => createMeta(config, frontmatter),
        [config, frontmatter],
    );

    return (
        <MarkdownLayoutContext.Provider value={ctx}>
            <Helmet>
                <title>{meta.title}</title>
                <meta name="description" content={meta.description} />
                <meta property="og:image" content={meta.openGraph.image.src} />
                <meta
                    property="og:image:alt"
                    content={meta.openGraph.image.altText}
                />
            </Helmet>
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
                            LayoutBackToTop,
                            ...mdx?.components,
                        }}
                    >
                        {mainContent ?? children}
                    </MDXProvider>
                    {backToTop && !isFullWidth && <LayoutBackToTop />}
                </article>
            )}
        </MarkdownLayoutContext.Provider>
    );
}

export default MarkdownLayout;
