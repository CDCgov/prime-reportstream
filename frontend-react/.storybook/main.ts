import type { StorybookConfig } from "@storybook/react-vite";
import remarkGfm from "remark-gfm";
import rehypeSlug from "rehype-slug";
import remarkToc from "remark-toc";
import remarkFrontmatter from "remark-frontmatter";
import remarkMdxFrontmatter from "remark-mdx-frontmatter";

const config: StorybookConfig = {
    stories: [
        "../src/**/*.mdx",
        "../src/**/*.stories.@(js|jsx|ts|tsx)",
        "./UtilizedUSWDSComponents/*.stories.@(js|jsx|ts|tsx)",
    ],
    addons: [
        "@storybook/addon-links",
        "@storybook/addon-essentials",
        "@storybook/addon-interactions",
        "@storybook/addon-a11y",
        {
            name: "@storybook/addon-docs",
            options: {
                mdxPluginOptions: {
                    mdxCompileOptions: {
                        remarkPlugins: [
                            remarkGfm,
                            remarkToc,
                            remarkFrontmatter,
                            remarkMdxFrontmatter,
                        ],
                        rehypePlugins: [rehypeSlug],
                    },
                },
            },
        },
    ],
    staticDirs: ["../public"],
    framework: {
        name: "@storybook/react-vite",
        options: {},
    },
    core: {},
    features: {
        buildStoriesJson: true,
    },
    async viteFinal(config, options) {
        // Exclude our mdx plugin from vite config in favor of storybook's
        config.plugins = config.plugins?.filter(
            (x: any, i) => x.name !== "@mdx-js/rollup"
        );

        return {
            ...config,
            build: {
                ...config.build,
                // Disable sourcemap to prevent out of memory
                // error on github actions
                sourcemap: false,
            },
        };
    },
    docs: {
        autodocs: "tag",
    },
};

export default config;
