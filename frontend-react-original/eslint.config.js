import { fixupPluginRules } from "@eslint/compat";
import eslint from "@eslint/js";
import configPrettier from "eslint-config-prettier";
import _import from "eslint-plugin-import";
import jestDom from "eslint-plugin-jest-dom";
import jsxA11y from "eslint-plugin-jsx-a11y";
import playwright from "eslint-plugin-playwright";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import storybook from "eslint-plugin-storybook";
import testingLibrary from "eslint-plugin-testing-library";
import vitest from "eslint-plugin-vitest";
import tseslint from "typescript-eslint";

const ignoredFiles = [
    "build/**/*",
    "vite.config.ts",
    "lint-staged.config.js",
    "scripts/**/*",
    "playwright.config.ts",
    "jest.config.ts",
    "coverage/**/*",
    "storybook-static/**/*",
    "e2e-data/**/*",
    ".storybook/**/*",
];

const defaultConfig = {
    extends: [
        eslint.configs.recommended,
        ...tseslint.configs.recommendedTypeChecked,
        ...tseslint.configs.stylisticTypeChecked,
        jsxA11y.flatConfigs.recommended,
        react.configs.flat?.recommended,
        react.configs.flat?.["jsx-runtime"],
        _import.flatConfigs.recommended,
        _import.flatConfigs.react,
        _import.flatConfigs.typescript,
        configPrettier,
    ],
    plugins: {
        "react-hooks": fixupPluginRules(reactHooks),
        "react-refresh": reactRefresh,
    },
    ignores: ignoredFiles,
    languageOptions: {
        parserOptions: {
            projectService: true,
            tsconfigRootDir: import.meta.dirname,
            ecmaFeatures: {
                jsx: true,
            },
        },
    },
    settings: {
        react: {
            version: "detect",
        },

        "import/resolver": {
            typescript: true,
            node: true,
        },
    },
    rules: {
        "jsx-a11y/no-autofocus": ["warn"],

        "react-refresh/only-export-components": [
            "off",
            {
                allowConstantExport: true,
            },
        ],

        "react/prop-types": ["warn"],
        "@typescript-eslint/no-unsafe-call": ["off"],
        "@typescript-eslint/no-unsafe-member-access": ["off"],
        "@typescript-eslint/no-unsafe-return": ["off"],
        "@typescript-eslint/no-unsafe-argument": ["off"],
        "@typescript-eslint/no-unsafe-assignment": ["off"],
        "import/named": ["off"],
        "import/namespace": ["off"],
        "import/default": ["off"],
        "import/no-named-as-default-member": ["off"],
        "import/no-unresolved": ["off"],
        indent: ["off"],
        "@typescript-eslint/indent": ["off"],
        "require-await": "off",

        "no-console": [
            "error",
            {
                allow: ["warn", "error", "info", "trace"],
            },
        ],

        "@typescript-eslint/no-explicit-any": ["off"],
        "no-unused-vars": "off",

        "@typescript-eslint/no-unused-vars": [
            "error",
            {
                vars: "all",
                varsIgnorePattern: "^_",
                args: "after-used",
                argsIgnorePattern: "^_",
                caughtErrors: "all",
                caughtErrorsIgnorePattern: "^_",
            },
        ],

        "import/order": [
            1,
            {
                groups: ["external", "builtin", "internal", "sibling", "parent", "index"],

                pathGroups: [
                    {
                        pattern: "components",
                        group: "internal",
                    },
                    {
                        pattern: "common",
                        group: "internal",
                    },
                    {
                        pattern: "routes/**",
                        group: "internal",
                    },
                    {
                        pattern: "assets/**",
                        group: "internal",
                        position: "after",
                    },
                ],

                pathGroupsExcludedImportTypes: ["internal"],

                alphabetize: {
                    order: "asc",
                    caseInsensitive: true,
                },
            },
        ],

        "sort-imports": [
            "error",
            {
                ignoreCase: true,
                ignoreDeclarationSort: true,
            },
        ],

        "@typescript-eslint/prefer-nullish-coalescing": ["error"],

        "@typescript-eslint/no-empty-object-type": [
            "error",
            {
                allowInterfaces: "always",
            },
        ],

        /* React-Hooks Recommended */
        "react-hooks/rules-of-hooks": "error",
        "react-hooks/exhaustive-deps": "warn",
    },
};

export default tseslint.config(
    defaultConfig,
    /* Vitest */
    {
        ...defaultConfig,
        files: ["src/**/__tests__/**/*.[jt]s?(x)", "src/**/?(*.)+(spec|test).[jt]s?(x)"],
        extends: [
            ...defaultConfig.extends,
            testingLibrary.configs["flat/react"],
            vitest.configs.recommended,
            jestDom.configs["flat/recommended"],
        ],
        rules: {
            ...defaultConfig.rules,
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
        ...defaultConfig,
        files: ["src/**/?(*.)+(stories).[jt]s?(x)"],
        extends: [...defaultConfig.extends, ...storybook.configs["flat/recommended"]],
    },
    /* Playwright */
    {
        ...defaultConfig,
        files: ["e2e/**/?(*.)+(spec|test).[jt]s"],
        extends: [...defaultConfig.extends, playwright.configs["flat/recommended"]],
        rules: {
            // TODO: investigate these for reconsideration or per-module ignoring
            "playwright/no-conditional-in-test": ["off"],
            "playwright/no-force-option": ["off"],
            "playwright/expect-expect": ["off"],

            "react-hooks/rules-of-hooks": "off",
        },
    },
);
