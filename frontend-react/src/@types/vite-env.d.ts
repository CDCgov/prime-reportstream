/// <reference types="vite/client" />
/// <reference types="vite-plugin-svgr/client" />

declare module "*.mdx" {
    export const frontmatter: Frontmatter;

    export const toc: TocEntry[];
}
