const sassResourcesLoader = require("craco-sass-resources-loader");

module.exports = {
    plugins: [
        {
            plugin: sassResourcesLoader,
            options: {
                resources: ["./src/global.scss"]
            },
        },
    ],
};
