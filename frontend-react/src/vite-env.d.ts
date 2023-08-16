/// <reference types="vite/client" />
/// <reference types="vite-plugin-svgr/client" />

declare module "*.mdx" {
    export const frontmatter: {
        sidenav?: string;
        breadcrumbs?: Array<{ label: string; href: string }>;
        title?: string;
        subtitle?: string | string[];
        callToAction?: Array<{
            label: string;
            href: string;
            icon: string;
            style: string;
        }>;
        lastUpdated?: string;
        toc?: boolean | { depth?: number };
        backToTop?: boolean;
    };

    export const toc: TocEntry[];
}
