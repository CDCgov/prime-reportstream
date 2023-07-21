import type { StorybookConfig } from "@storybook/react-vite";
import remarkGfm from "remark-gfm";
import remarkToc from "remark-gfm";
import path from "node:path";

const config: StorybookConfig = {
    stories: [
        "../src/**/*.stories.mdx",
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
                        remarkPlugins: [remarkGfm, remarkToc],
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
    async viteFinal(config, options) {
        // Exclude our mdx plugin from vite config in favor of storybook's
        config.plugins = config.plugins?.filter(
            (x: any, i) => x.name !== "@mdx-js/rollup"
        );

        // Proxy react-uswds storybook website locally so we can supply
        // locally-created stories.json file so that it works on sb 7
        if (!config.server) {
            config.server = {};
        }
        if (!config.server.proxy) {
            config.server.proxy = {};
        }
        if (!config.server.fs) {
            config.server.fs = {};
        }
        config.server.fs.allow = [".."];

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
