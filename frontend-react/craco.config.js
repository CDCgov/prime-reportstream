const sassResourcesLoader = require("craco-sass-resources-loader");

module.exports = {
    plugins: [
        {
            plugin: sassResourcesLoader,
            options: {
                resources: ["./node_modules/uswds/dist/scss/packages/_required.scss"],
            },
        },
    ],
};
