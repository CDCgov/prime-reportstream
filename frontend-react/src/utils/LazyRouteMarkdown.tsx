import MarkdownLayout from "../layouts/Markdown/MarkdownLayout";

import MDXImports from "./MDXImports";

/**
 * Creates react-router-compatible lazy function for provided file path.
 */
export function lazyRouteMarkdown(path: string) {
    return async () => {
        const importer = MDXImports[`../${path}.mdx`];

        if (!importer) {
            throw new Error(`MDX importer not found for: ${path}`);
        }

        const module = await importer();
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
