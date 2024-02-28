const babelJest = require("babel-jest").default;
const importMetaBabelPlugin = require("./importMetaBabelPlugin.cjs");

module.exports = babelJest.createTransformer({
    presets: [
        "@babel/preset-env",
        ["@babel/preset-react", { runtime: "automatic", development: true }],
        ["@babel/preset-typescript"],
    ],
    plugins: [[importMetaBabelPlugin]],
});
