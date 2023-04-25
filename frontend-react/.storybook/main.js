"use strict";

const remarkPluginFiles = ["remark-gfm"];
const rehypePluginFiles = ["rehype-slug"];

async function getPlugins(fileNames) {
    return await Promise.all(
        fileNames.map((n) => import(n).then((m) => m.default))
    );
}

/** @type {import('@storybook/core-common').StorybookConfig} */
module.exports = {
    stories: [
        "../src/**/*.stories.mdx",
        "../src/**/*.stories.@(js|jsx|ts|tsx)",
    ],
    features: {
        previewMdx2: true,
    },
    addons: [
        "@storybook/addon-links",
        "@storybook/addon-essentials",
        "@storybook/addon-interactions",
        "@storybook/addon-a11y",
    ],
    framework: "@storybook/react",
    core: {
        builder: "@storybook/builder-webpack5",
    },
    refs: {
        trussworks: {
            title: "Trussworks Storybook",
            url: "https://trussworks.github.io/react-uswds/",
        },
    },
    features: {
        previewMdx2: true,
        babelModeV7: true,
        storyStoreV7: true,
        modernInlineRender: true,
        argTypeTargetsV7: true,
        breakingChangesV7: true,
    },
    webpackFinal: async (config) => {
        /**
         * Inject our wanted mdx plugins
         */
        const remarkPlugins = await getPlugins(remarkPluginFiles);
        const rehypePlugins = await getPlugins(rehypePluginFiles);

        // non-story mdx rule has an exclude clause
        const mdxRule = config.module.rules.find(
            (r) => r.test?.toString().includes(".mdx") && r.exclude
        );
        const loader = mdxRule?.use.find((u) => u.loader.includes("mdx2-csf"));

        if (!loader.options.mdxCompileOptions) {
            loader.options.mdxCompileOptions = {};
        }

        loader.options.mdxCompileOptions.remarkPlugins = remarkPlugins;
        loader.options.mdxCompileOptions.rehypePlugins = rehypePlugins;

        return config;
    },
};
