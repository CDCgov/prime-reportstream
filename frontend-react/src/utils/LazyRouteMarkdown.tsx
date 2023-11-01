import type { MDXModule } from "mdx/types";
import React from "react";

const MarkdownLayout = React.lazy(
    () => import("../layouts/Markdown/MarkdownLayout"),
);

/**
 * Creates React.lazy-compatible function that renders a content page
 */
export function lazyRouteMarkdown(fn: () => Promise<MDXModule>) {
    return async () => {
        const module = await fn();
        const Content = module.default;
        return {
            default: () => (
                <MarkdownLayout {...module}>
                    <Content />
                </MarkdownLayout>
            ),
        };
    };
}
