module.exports = {
    extends: [
        "react-app",
        "plugin:import/recommended",
        "plugin:import/typescript",
        "prettier",
    ],
    plugins: ["testing-library", "unused-imports", "jest-dom", "prettier"],
    env: {
        browser: true,
        node: false,
        es6: true,
    },
    globals: {
        RequestInit: true,
        process: "readonly",
        cy: "readonly",
    },
    rules: {
        "import/no-unresolved": "off",
        "import/first": "warn",
        "import/order": [
            "warn",
            {
                "newlines-between": "always",
                pathGroups: [
                    {
                        pattern: "~/**",
                        group: "external",
                        position: "after",
                    },
                ],
            },
        ],
        "import/newline-after-import": "warn",
        "import/no-commonjs": "off",
        "import/no-named-as-default": "off",
        "prettier/prettier": "error",
        "arrow-body-style": "off",
        "prefer-arrow-callback": "off",
        "unused-imports/no-unused-imports": "error",
        "unused-imports/no-unused-vars": [
            "warn",
            {
                vars: "all",
                varsIgnorePattern: "^_",
                args: "after-used",
                argsIgnorePattern: "^_",
            },
        ],
        camelcase: [
            "warn",
            {
                allow: ["days_to_show", "organization_id", "sending_at"],
            },
        ],
    },
    overrides: [
        {
            files: ["**/*.stories.*"],
            rules: {
                "import/no-anonymous-default-export": "off",
            },
        },
        {
            files: [
                "./src/**/__tests__/**/*.[jt]s?(x)",
                "./src/**/?(*.)+(spec|test).[jt]s?(x)",
            ],
            extends: [
                "plugin:testing-library/react",
                "plugin:jest-dom/recommended",
            ],
            rules: {
                "testing-library/no-render-in-setup": [
                    "error",
                    {
                        allowTestingFrameworkSetupHook: "beforeEach",
                    },
                ],
                "testing-library/no-node-access": "off",
                "testing-library/prefer-screen-queries": "warn",
                "testing-library/no-unnecessary-act": "warn",
                "testing-library/no-await-sync-query": "warn",
            },
        },
        {
            plugins: ["chai-friendly"],
            files: ["**/cypress/**/*.[jt]s?(x)"],
            extends: ["plugin:cypress/recommended"],
            rules: {
                "no-unused-expressions": "off",
                "@typescript-eslint/no-unused-expressions": "off",
                "chai-friendly/no-unused-expressions": "error",
            },
        },
    ],
    settings: {
        "import/resolver": {},
    },
    ignorePatterns: ["node_modules/", "build/", "src/reportWebVitals.js"],
};
