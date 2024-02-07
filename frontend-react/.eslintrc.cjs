module.exports = {
    root: true,
    env: { browser: true, es2020: true },
    extends: [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended",
        "plugin:react-hooks/recommended",
        "plugin:react/recommended",
        "plugin:react/jsx-runtime",
        "plugin:jsx-a11y/recommended",
        "plugin:import/recommended",
        "plugin:import/typescript",
        "prettier",
    ],
    ignorePatterns: ["build", ".eslintrc.cjs"],
    parser: "@typescript-eslint/parser",
    parserOptions: {
        ecmaFeatures: {
            jsx: true,
        },
    },
    plugins: ["react-refresh"],
    settings: {
        react: {
            version: "detect",
        },
        "import/resolver": {
            typescript: true,
            node: true,
        },
    },
    overrides: [
        {
            files: [
                "**/__tests__/**/*.[jt]s?(x)",
                "**/?(*.)+(spec|test).[jt]s?(x)",
            ],
            extends: [
                "plugin:testing-library/react",
                "plugin:jest/recommended",
                "plugin:jest-dom/recommended",
            ],
            rules: {
                "testing-library/no-node-access": ["warn"], // TODO: remove line and fix errors
                "testing-library/no-await-sync-events": [
                    "error",
                    { eventModules: ["fire-event"] },
                ],
                "testing-library/no-render-in-lifecycle": "error",
                "testing-library/prefer-screen-queries": "warn",
                "testing-library/no-unnecessary-act": "warn",
                "testing-library/no-await-sync-queries": "warn",
                "jest/no-mocks-import": ["warn"], // TODO: remove line and fix errors
            },
        },
        {
            files: ["**/?(*.)+(stories).[jt]s?(x)"],
            extends: ["plugin:storybook/recommended"],
        },
    ],
    rules: {
        "no-console": ["error", { allow: ["warn", "error", "info", "trace"] }],
        "react-refresh/only-export-components": [
            "warn",
            { allowConstantExport: true },
        ],
        "@typescript-eslint/no-explicit-any": ["off"],
        "@typescript-eslint/no-unused-vars": [
            "error",
            { varsIgnorePattern: "^_", argsIgnorePattern: "^_" },
        ],
        "jsx-a11y/no-autofocus": ["warn"], // TODO: remove line and fix errors
        "react/prop-types": ["warn"], // TODO: remove line and fix errors
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
    },
};
