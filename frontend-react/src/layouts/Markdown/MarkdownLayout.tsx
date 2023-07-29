import {
    Breadcrumb,
    BreadcrumbBar,
    Grid,
    GridContainer,
} from "@trussworks/react-uswds";
import { MDXProvider } from "@mdx-js/react";
import { Helmet } from "react-helmet-async";
import React, {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useState,
} from "react";
import * as reactUSWDS from "@trussworks/react-uswds";
import classNames from "classnames";
import type { TocEntry } from "remark-mdx-toc";
import Slugger from "github-slugger";

import { USCrumbLink, USSmartLink, USNavLink } from "../../components/USLink";
import * as shared from "../../shared";
import MDXModules from "../../MDXModules";
import {
    InPageNav,
    InPageNavHeader,
    InPageNavList,
    InPageNavListItem,
    InPageNavNav,
} from "../../shared/InPageNav";

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
    };
    toc?: TocEntry[];
    main?: JSX.Element;
    nav?: JSX.Element;
    mdx?: React.ComponentProps<typeof MDXProvider>;
}

export const LayoutSidenav = ({ children }: { children: React.ReactNode }) => {
    const { setSidenavContent } = useContext(MarkdownLayoutContext);

    useEffect(() => {
        setSidenavContent(children);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

export const LayoutMain = ({ children }: { children: React.ReactNode }) => {
    const { setMainContent } = useContext(MarkdownLayoutContext);

    useEffect(() => {
        setMainContent(children);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

export const CallToAction = ({ label, href, style, icon }) => {
    const classname = classNames(
        "usa-button",
        style ? `usa-button--${style}` : null
    );
    return (
        <USSmartLink href={href} className={classname}>
            {label}
            {icon ? (
                <sharedComponents.Icon name={icon} className="usa-icon--auto" />
            ) : null}
        </USSmartLink>
    );
};

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
    Subtitle: (props) => {
        return <p role="doc-subtitle">{props.children}</p>;
    },
};

const MarkdownLayoutContext = createContext<{
    sidenavContent?: React.ReactNode;
    setSidenavContent: (jsx: React.ReactNode) => void;
    mainContent?: React.ReactNode;
    setMainContent: (jsx: React.ReactNode) => void;
}>({} as any);

function TableOfContentsEntry({
    children,
    depth,
    value,
    attributes,
    maxDepth = 6,
    isInPage = false,
}: SluggedTocEntry & { maxDepth?: number; isInPage?: boolean }) {
    const Wrapper = isInPage ? InPageNavListItem : "li";

    return (
        <Wrapper>
            <USSmartLink href={`#${attributes.id}`}>{value}</USSmartLink>
            {children && depth + 1 <= maxDepth ? (
                <TableOfContents
                    items={children}
                    depth={maxDepth}
                    isInPage={isInPage}
                />
            ) : null}
        </Wrapper>
    );
}

const slugger = new Slugger();

interface SluggedTocEntry extends TocEntry {
    children: SluggedTocEntry[];
    attributes: {
        id: string;
    };
}

function sluggifyToc(items: TocEntry[]) {
    const sluggedItems = items.map((i) => {
        return {
            ...i,
            children: i.children ? sluggifyToc(i.children) : i.children,
            attributes: i.attributes.id
                ? i.attributes
                : {
                      ...i.attributes,
                      id: slugger.slug(i.value),
                  },
        };
    }) as SluggedTocEntry[];

    slugger.reset();

    return sluggedItems;
}

function TableOfContents({
    items,
    depth = 6,
    isInPage = false,
}: {
    depth?: number;
    items: TocEntry[];
    isInPage?: boolean;
}) {
    const sluggedItems = sluggifyToc(items);
    const listItems = sluggedItems
        .filter((i) => i.depth <= depth)
        .map((i) => (
            <TableOfContentsEntry
                key={i.attributes.id}
                {...i}
                maxDepth={depth}
                isInPage={isInPage}
            />
        ));
    const Wrapper = isInPage ? InPageNavList : "ul";

    return <Wrapper>{listItems}</Wrapper>;
}

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

    const tableOfContentsList =
        toc && tocEntries ? (
            <TableOfContents
                {...(typeof toc === "object" ? toc : {})}
                items={tocEntries}
            />
        ) : null;
    const tableOfContents =
        toc && toc.isInPage ? (
            <InPageNav>
                <InPageNavNav>
                    <InPageNavHeader>Jump to:</InPageNavHeader>
                    {tableOfContentsList}
                </InPageNavNav>
            </InPageNav>
        ) : null;

    return (
        <MarkdownLayoutContext.Provider value={ctx}>
            {helmet}
            <GridContainer className="rs-prose">
                <Grid row className="flex-justify flex-align-start">
                    {sidenavContent || tableOfContents ? (
                        tableOfContents ? (
                            tableOfContents
                        ) : (
                            <nav
                                aria-label="side-navigation"
                                className="tablet:grid-col-3 position-sticky top-0"
                            >
                                <MDXProvider
                                    components={MDXComponents}
                                    {...mdx}
                                >
                                    {sidenavContent}
                                </MDXProvider>
                            </nav>
                        )
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
                                    {subtitle ? (
                                        Array.isArray(subtitle) ? (
                                            subtitle.map((s, i) => (
                                                <p
                                                    key={i}
                                                    className="usa-intro text-base"
                                                >
                                                    {s}
                                                </p>
                                            ))
                                        ) : (
                                            <p className="usa-intro text-base">
                                                {subtitle}
                                            </p>
                                        )
                                    ) : null}
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
