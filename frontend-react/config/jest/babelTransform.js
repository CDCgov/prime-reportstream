const babelJest = require("babel-jest").default;
const importMetaBabelPlugin = require("./importMetaBabelPlugin");

module.exports = babelJest.createTransformer({
    presets: [
        /*[
            require.resolve("../babel"),
            {
                runtime: "automatic",
            },
        ],*/
        "@babel/preset-env",
        ["@babel/preset-react", { runtime: "automatic", development: true }],
        ["@babel/preset-typescript"],
    ],
    plugins: [[importMetaBabelPlugin]],
    babelrc: false,
    configFile: false,
});
