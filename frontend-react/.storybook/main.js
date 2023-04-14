"use strict";

/** @type {import('@storybook/core-common').StorybookConfig} */
module.exports = {
    stories: [
        "../src/**/*.stories.mdx",
        "../src/**/*.stories.@(js|jsx|ts|tsx)",
    ],
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
};
