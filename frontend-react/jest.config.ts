import type { Config } from "@jest/types";

// Do this as the first thing so that any code reading it knows the right env.
process.env.BABEL_ENV = "test";
process.env.NODE_ENV = "test";
process.env.PUBLIC_URL = "";
process.env.TZ = "UTC";

// Ensure environment variables are read.
// eslint-disable-next-line @typescript-eslint/no-unused-vars, unused-imports/no-unused-imports, import/first
import _ from "./config/env.cjs";

const config: Config.InitialOptions = {
    roots: ["<rootDir>/src"],
    collectCoverageFrom: ["src/**/*.{js,jsx,ts,tsx}", "!src/**/*.d.ts"],
    setupFilesAfterEnv: ["<rootDir>/src/setupTests.ts"],
    testMatch: [
        "<rootDir>/src/**/__tests__/**/*.{js,jsx,ts,tsx}",
        "<rootDir>/src/**/*.{spec,test}.{js,jsx,ts,tsx}",
    ],
    testEnvironment: "jsdom",
    transform: {
        "^.+\\.(js|jsx|mjs|cjs|ts|tsx)$":
            "<rootDir>/config/jest/babelTransform.cjs",
        "^.+\\.css$": "<rootDir>/config/jest/cssTransform.cjs",
        "^(?!.*\\.(js|jsx|mjs|cjs|ts|tsx|css|json)$)":
            "<rootDir>/config/jest/fileTransform.cjs",
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
        "\\.(css|less|scss)$": "identity-obj-proxy",
        "@mdx-js/react": "<rootDir>/src/__mocks__/mdxjsReactMock.tsx",
        // remove vite-supported queries from url imports
        "^(.+)\\?.*": "$1",
        "^.*\\.mdx": "<rootDir>/src/__mocks__/mdxFrontmatterMock.tsx",
        "^react-helmet-async$":
            "<rootDir>/src/__mocks__/reactHelmetAsyncMock.tsx",
        "MDXImports(\\.ts)?$": "<rootDir>/src/__mocks__/mdxModulesMock.ts",
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
