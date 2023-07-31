import {
    Breadcrumb,
    BreadcrumbBar,
    Grid,
    GridContainer,
} from "@trussworks/react-uswds";
import { MDXProvider } from "@mdx-js/react";
import { Helmet } from "react-helmet-async";
import React, { useMemo, useState } from "react";
import * as reactUSWDS from "@trussworks/react-uswds";
import type { TocEntry } from "remark-mdx-toc";

import { USCrumbLink, USSmartLink, USNavLink } from "../../components/USLink";
import * as shared from "../../shared";
import MDXModules from "../../MDXModules";

import { TableOfContents } from "./TableOfContents";
import { CallToAction } from "./CallToAction";
import MarkdownLayoutContext from "./Context";
import { LayoutSidenav, LayoutMain } from "./LayoutComponents";

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
    children: JSX.Element;
    frontmatter?: {
        sidenav?: string;
        breadcrumbs?: Array<{ label: string; href: string }>;
        title?: string;
        subtitle?: string | string[];
        callToAction?: Array<{ label: string; href: string }>;
        lastUpdated?: string;
        toc?: boolean | { depth?: number };
        backToTop?: boolean;
    };
    toc?: TocEntry[];
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
    Subtitle: (props: React.PropsWithChildren<{}>) => {
        return <p>{props.children}</p>;
    },
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
    } = {},
    toc: tocEntries,
}: MarkdownLayoutProps) {
    const helmet = title ? (
        <Helmet>
            <title>{title}</title>
        </Helmet>
    ) : null;
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

    const tableOfContents =
        toc && tocEntries ? (
            <TableOfContents
                {...(typeof toc === "object" ? toc : {})}
                items={tocEntries}
            />
        ) : null;

    const sub = subtitle ? (
        Array.isArray(subtitle) ? (
            subtitle.map((s, i) => (
                <p key={i} className="usa-intro text-base">
                    {s}
                </p>
            ))
        ) : (
            <p className="usa-intro text-base">{subtitle}</p>
        )
    ) : null;

    return (
        <MarkdownLayoutContext.Provider value={ctx}>
            {helmet}
            <GridContainer className="rs-prose">
                <Grid row className="flex-justify flex-align-start">
                    {sidenavContent ? (
                        <nav
                            aria-label="side-navigation"
                            className="tablet:grid-col-3 position-sticky top-0"
                        >
                            <MDXProvider components={MDXComponents} {...mdx}>
                                {sidenavContent}
                            </MDXProvider>
                        </nav>
                    ) : null}
                    {main ?? (
                        <main
                            className={
                                sidenavContent || tableOfContents
                                    ? "tablet:grid-col-8"
                                    : "tablet:grid-col-12"
                            }
                        >
                            <header>
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
                                <hgroup>
                                    <h1>{title}</h1>
                                    {sub}
                                </hgroup>
                                {callToAction
                                    ? callToAction.map((c) => (
                                          <CallToAction {...c} />
                                      ))
                                    : null}
                                {lastUpdated ? (
                                    <p className="text-base text-italic">
                                        Last updated: {lastUpdated}
                                    </p>
                                ) : null}
                            </header>
                            {tableOfContents ? (
                                <>
                                    <b>On this page:</b>
                                    {tableOfContents}
                                    <hr />
                                </>
                            ) : null}
                            <MDXProvider
                                components={{
                                    ...MDXComponents,
                                    LayoutSidenav,
                                    LayoutMain,
                                }}
                                {...mdx}
                            >
                                {mainContent ?? children}
                            </MDXProvider>
                            {backToTop ? (
                                <p>
                                    <USSmartLink href="#top">
                                        Back to top
                                    </USSmartLink>
                                </p>
                            ) : null}
                        </main>
                    )}
                </Grid>
            </GridContainer>
        </MarkdownLayoutContext.Provider>
    );
}

export default MarkdownLayout;

/**
 * Creates react-router-compatible lazy function for provided file path.
 */
export function lazyRouteMarkdown(path: string) {
    return async () => {
        const module = await MDXModules[`./${path}.mdx`]();
        const Content = module.default;
        return {
            Component() {
                return (
                    <MarkdownLayout {...module}>
                        <Content />
                    </MarkdownLayout>
                );
            },
        };
    };
}
