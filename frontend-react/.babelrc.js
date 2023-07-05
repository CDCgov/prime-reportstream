// FUTURE_TODO: Simplify babel config into this file
/**
 * This is currently only being used for IDEs and storybook.
 * Everything else is flowing through CRA-sourced scripts.
 */

const create = require("./config/babel/prod");

module.exports = function (api) {
    // Unsure why this is needed since a plain config object doesn't cause an error to occur
    api.cache(true);

    return create(api);
};
