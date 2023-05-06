import type { StorybookConfig } from "@storybook/react-vite";
import remarkGfm from "remark-gfm";
import remarkToc from "remark-gfm";

const config: StorybookConfig = {
    stories: [
        "../src/**/*.stories.mdx",
        "../src/**/*.stories.@(js|jsx|ts|tsx)",
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
    refs: {
        trussworks: {
            title: "Trussworks Storybook",
            url: "https://trussworks.github.io/react-uswds/",
        },
    },
    features: {
        buildStoriesJson: true,
    },
    async viteFinal(config, options) {
        // Exclude our mdx plugin from vite config in favor of storybook's
        config.plugins = config.plugins?.filter(
            (x: any, i) => x.name !== "@mdx-js/rollup"
        );
        return config;
    },
    docs: {
        autodocs: true,
    },
};
export default config;
