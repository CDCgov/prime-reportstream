module.exports = {
  stories: ["../src/**/*.stories.mdx", "../src/**/*.stories.@(js|jsx|ts|tsx)"],
  addons: ["@storybook/addon-links", "@storybook/addon-essentials", "@storybook/addon-interactions", "@storybook/addon-a11y", "@storybook/addon-mdx-gfm"],
  framework: {
    name: "@storybook/react-webpack5",
    options: {}
  },
  refs: {
    trussworks: {
      title: "Trussworks Storybook",
      url: "https://trussworks.github.io/react-uswds/"
    }
  },
  docs: {
    autodocs: true
  }
};