const fs = require("fs");

const paths = require("./paths.cjs");

// Make sure that including paths.js after env.js will read .env variables.
delete require.cache[require.resolve("./paths.cjs")];

const NODE_ENV = process.env.NODE_ENV;
if (!NODE_ENV) {
    throw new Error(
        "The NODE_ENV environment variable is required but was not specified."
    );
}

// https://github.com/bkeepers/dotenv#what-other-env-files-can-i-use
const dotenvFiles = [
    `${paths.dotenv}.${NODE_ENV}.local`,
    // Don't include `.env.local` for `test` environment
    // since normally you expect tests to produce the same
    // results for everyone
    NODE_ENV !== "test" && `${paths.dotenv}.local`,
    `${paths.dotenv}.${NODE_ENV}`,
    paths.dotenv,
].filter(Boolean);

// Load environment variables from .env* files. Suppress warnings using silent
// if this file is missing. dotenv will never modify any environment variables
// that have already been set.  Variable expansion is supported in .env files.
// https://github.com/motdotla/dotenv
// https://github.com/motdotla/dotenv-expand
dotenvFiles.forEach((dotenvFile) => {
    if (fs.existsSync(dotenvFile)) {
        require("dotenv-expand").expand(
            require("dotenv").config({
                path: dotenvFile,
            })
        );
    }
});

// Grab NODE_ENV and VITE_* environment variables and prepare them to be
// injected into the application.
const VITE = /^VITE_/i;

function getClientEnvironment() {
    const nodeEnv = process.env.NODE_ENV || "development";
    const envs = ["production", "development", "test"];

    const raw = Object.keys(process.env)
        .filter((key) => VITE.test(key))
        .reduce(
            (env, key) => {
                env[key] = process.env[key];
                return env;
            },
            {
                NODE_ENV: envs.indexOf(nodeEnv) ? nodeEnv : "production",
            }
        );
    // Stringify all values so we can feed into webpack DefinePlugin
    const stringified = {
        "import.meta.env": Object.keys(raw).reduce((env, key) => {
            env[key] = JSON.stringify(raw[key]);
            return env;
        }, {}),
    };

    return { raw, stringified };
}

module.exports = getClientEnvironment;
