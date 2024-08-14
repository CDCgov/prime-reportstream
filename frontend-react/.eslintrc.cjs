module.exports = {
    root: true,
    env: { browser: true, es2020: true },
    extends: [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended-type-checked",
        "plugin:@typescript-eslint/stylistic-type-checked",
        "plugin:react-hooks/recommended",
        "plugin:react/recommended",
        "plugin:react/jsx-runtime",
        "plugin:jsx-a11y/recommended",
        "plugin:import/recommended",
        "plugin:import/typescript",
        "prettier",
    ],
    ignorePatterns: [
        "build",
        ".eslintrc.cjs",
        "vite.config.ts",
        "lint-staged.config.js",
        "scripts",
        "playwright.config.ts",
        "jest.config.ts",
        "coverage",
        "storybook-static",
        "e2e-data",
    ],
    parser: "@typescript-eslint/parser",
    parserOptions: {
        ecmaFeatures: {
            jsx: true,
        },
        project: true,
        tsconfigRootDir: __dirname,
    },
    plugins: ["react-refresh", "@typescript-eslint", "react-hooks", "react", "jsx-a11y", "import"],
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
        /* Vitest */
        {
            files: ["./src/**/__tests__/**/*.[jt]s?(x)", "./src/**/?(*.)+(spec|test).[jt]s?(x)"],
            extends: [
                "plugin:testing-library/react",
                "plugin:vitest/legacy-recommended",
                "plugin:jest-dom/recommended",
            ],
            rules: {
                /* Temporarily changed to warnings or disabled pending future work */
                "testing-library/no-node-access": ["warn"],
                "vitest/no-mocks-import": ["warn"],
                "vitest/expect-expect": ["warn"],

                /* Tweaks for plugin conflicts */
                "@typescript-eslint/unbound-method": "off",

                /* Custom project rules */
                "testing-library/no-await-sync-events": ["error", { eventModules: ["fire-event"] }],
                "testing-library/no-render-in-lifecycle": "error",
                "testing-library/prefer-screen-queries": "warn",
                "testing-library/no-unnecessary-act": "warn",
                "testing-library/no-await-sync-queries": "warn",
            },
        },
        /* Storybook */
        {
            files: ["./src/**/?(*.)+(stories).[jt]s?(x)"],
            extends: ["plugin:storybook/recommended"],
        },
        /* Playwright */
        {
            files: ["./e2e/**/?(*.)+(spec|test).[jt]s"],
            extends: ["plugin:playwright/recommended"],
            rules: {
                // TODO: investigate these for reconsideration or per-module ignoring
                "playwright/no-conditional-in-test": ["off"],
                "playwright/no-force-option": ["off"],
            },
        },
    ],
    rules: {
        /* Temporarily changed to warnings or disabled pending future work */
        "jsx-a11y/no-autofocus": ["warn"],
        "react-refresh/only-export-components": ["off", { allowConstantExport: true }],

        // Requires extensive updates to types in code, however SHOULD BE ENABLED EVENTUALLY
        "react/prop-types": ["warn"],
        "@typescript-eslint/no-unsafe-call": ["off"],
        "@typescript-eslint/no-unsafe-member-access": ["off"],
        "@typescript-eslint/no-unsafe-return": ["off"],
        "@typescript-eslint/no-unsafe-argument": ["off"],
        "@typescript-eslint/no-unsafe-assignment": ["off"],

        /* Tweaks for plugin conflicts */
        "import/named": ["off"],
        "import/namespace": ["off"],
        "import/default": ["off"],
        "import/no-named-as-default-member": ["off"],
        "import/no-unresolved": ["off"],
        indent: ["off"],
        "@typescript-eslint/indent": ["off"],
        "require-await": "off",

        /* Custom project rules */
        "no-console": ["error", { allow: ["warn", "error", "info", "trace"] }],
        "@typescript-eslint/no-explicit-any": ["off"],
        "@typescript-eslint/no-unused-vars": [
            "error",
            {
                vars: "all",
                varsIgnorePattern: "^_",
                args: "after-used",
                argsIgnorePattern: "^_",
            },
        ],
        "import/order": [
            1,
            {
                groups: ["external", "builtin", "internal", "sibling", "parent", "index"],
                pathGroups: [
                    { pattern: "components", group: "internal" },
                    { pattern: "common", group: "internal" },
                    { pattern: "routes/**", group: "internal" },
                    {
                        pattern: "assets/**",
                        group: "internal",
                        position: "after",
                    },
                ],
                pathGroupsExcludedImportTypes: ["internal"],
                alphabetize: { order: "asc", caseInsensitive: true },
            },
        ],
        "sort-imports": ["error", { ignoreCase: true, ignoreDeclarationSort: true }],
        "@typescript-eslint/prefer-nullish-coalescing": ["error"],
    },
};
