import type { StorybookConfig } from "@storybook/react-vite";
import remarkToc from "remark-mdx-toc";
import turbosnap from "vite-plugin-turbosnap";

const config: StorybookConfig = {
    stories: [
        "../src/**/*.stories.mdx",
        "../src/**/*.stories.@(js|jsx|ts|tsx)",
        "./UtilizedUSWDSComponents/*.stories.@(js|jsx|ts|tsx)",
    ],
    addons: [
        "storybook-addon-react-router-v6",
        "@storybook/addon-links",
        "@storybook/addon-essentials",
        "@storybook/addon-interactions",
        "@storybook/addon-a11y",
        {
            name: "@storybook/addon-docs",
            options: {
                mdxPluginOptions: {
                    mdxCompileOptions: {
                        remarkPlugins: [remarkToc],
                    },
                },
            },
        },
    ],
    framework: {
        name: "@storybook/react-vite",
        options: {},
    },
    core: {},
    features: {
        buildStoriesJson: true,
    },
    async viteFinal(config, { configType }) {
        // Exclude our mdx plugin from vite config in favor of storybook's
        config.plugins = config.plugins?.filter(
            (x: any, i) => x.name !== "@mdx-js/rollup",
        );

        if (configType === "PRODUCTION") {
            config.plugins?.push(
                turbosnap({
                    rootDir: config.root ?? process.cwd(),
                }),
            );
        }

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
