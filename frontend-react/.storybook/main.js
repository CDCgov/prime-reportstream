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
        {
            name: "@storybook/preset-scss",
            options: {
                sassLoaderOptions: {
                    sassOptions: {
                        includePaths: ["./node_modules/@uswds/uswds/packages"],
                    },
                },
            },
        },
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
};
