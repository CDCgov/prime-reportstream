const generateEnvObject = require("../env.cjs");

const template = require("@babel/template").default;

/**
 * Add import.meta.env support
 * Note: import.meta.url is not supported at this time
 */
module.exports = function () {
    const env = generateEnvObject();
    const ast = template.ast(`
  ({env: ${JSON.stringify(env.raw)}})
`);
    return {
        visitor: {
            MetaProperty(path, state) {
                path.replaceWith(ast);
            },
        },
    };
};
