/// <reference types="vite/client" />
/// <reference types="vite-plugin-svgr/client" />

interface OpenGraph {
    image: {
        src: string;
        altText: string;
    };
}
interface MetaData {
    title?: string;
    description?: string;
    openGraph?: OpenGraph;
}
interface Frontmatter {
    sidenav?: string;
    breadcrumbs?: { label: string; href: string }[];
    title?: string;
    subtitle?: string | string[];
    meta?: MetaData;
    callToAction?: {
        label: string;
        href: string;
        icon: string;
        style: string;
    }[];
    lastUpdated?: string;
    toc?: boolean | { depth?: number };
    backToTop?: boolean;
}

interface ContentSubitem
    extends React.PropsWithChildren<React.HTMLAttributes<HTMLElement>> {
    method?: number;
    title?: string;
    icon?: string;
    img?: string;
    imgAlt?: string;
    imgClassName?: string;
    summary?: string;
    items?: { title?: string; summary?: string }[];
}

interface ContentItem {
    title?: string;
    type?: string;
    summary?: string;
    subTitle?: string;
    bullets?: { content?: string }[];
    items?: ContentSubitem[];
    description?: string;
    buttonText?: string;
    buttonUrlSubject?: string;
    citation?: CitationItem[];
}

interface CitationItem {
    title?: string;
    quote?: string;
    author?: string;
    authorTitle?: string;
}

declare module "*.mdx" {
    export const frontmatter: Frontmatter;

    export const toc: TocEntry[];
}
