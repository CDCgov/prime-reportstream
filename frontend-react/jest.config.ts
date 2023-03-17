import type { Config } from "@jest/types";

// Do this as the first thing so that any code reading it knows the right env.
process.env.BABEL_ENV = "test";
process.env.NODE_ENV = "test";
process.env.PUBLIC_URL = "";
process.env.TZ = "UTC";

// Ensure environment variables are read.
require("./config/env");

const config: Config.InitialOptions = {
    roots: ["<rootDir>/src"],
    collectCoverageFrom: ["src/**/*.{js,jsx,ts,tsx}", "!src/**/*.d.ts"],
    setupFiles: ["react-app-polyfill/jsdom"],
    setupFilesAfterEnv: ["<rootDir>/src/setupTests.ts"],
    testMatch: [
        "<rootDir>/src/**/__tests__/**/*.{js,jsx,ts,tsx}",
        "<rootDir>/src/**/*.{spec,test}.{js,jsx,ts,tsx}",
    ],
    testEnvironment: "jsdom",
    transform: {
        "^.+\\.(js|jsx|mjs|cjs|ts|tsx)$":
            "<rootDir>/config/jest/babelTransform.js",
        "^.+\\.css$": "<rootDir>/config/jest/cssTransform.js",
        "^(?!.*\\.(js|jsx|mjs|cjs|ts|tsx|css|json)$)":
            "<rootDir>/config/jest/fileTransform.js",
    },
    transformIgnorePatterns: [
        "[/\\\\]node_modules[/\\\\](?!(react-markdown|vfile|vfile-message|markdown-table|unist-.*|unified|bail|is-plain-obj|trough|remark-.*|rehype-.*|html-void-elements|hast-util-.*|zwitch|hast-to-hyperscript|hastscript|web-namespaces|mdast-util-.*|escape-string-regexp|micromark.*|decode-named-character-reference|character-entities|property-information|hast-util-whitespace|space-separated-tokens|comma-separated-tokens|pretty-bytes|ccount|mdast-util-gfm|gemoji)).+\\.(js|jsx|mjs|cjs|ts|tsx)$",
    ],
    modulePaths: [],
    moduleNameMapper: {
        "^react-native$": "react-native-web",
        "^.+\\.module\\.(css|sass|scss)$": "identity-obj-proxy",
        "^@rest-hooks/(.*)": [
            "<rootDir>/node_modules/@rest-hooks/$1/dist/index.js",
            "<rootDir>/node_modules/@rest-hooks/$1/dist/$1.js",
            "<rootDir>/node_modules/@rest-hooks/$1/dist/index.cjs.js",
        ],
        "^msw/lib/node$": "<rootDir>/node_modules/msw/lib/node/index.js",
        "^rest-hooks$": "<rootDir>/node_modules/rest-hooks/dist/index.js",
        "\\.(css|less|scss)$": "<rootDir>/src/__mocks__/staticAssetMock.js",
    },
    moduleFileExtensions: [
        "web.js",
        "js",
        "web.ts",
        "ts",
        "web.tsx",
        "tsx",
        "json",
        "web.jsx",
        "jsx",
        "node",
    ],
    watchPlugins: [
        "jest-watch-typeahead/filename",
        "jest-watch-typeahead/testname",
    ],
    resetMocks: true,
};

export default config;
