// The ESLint browser environment defines all browser globals as valid,
// even though most people don't know some of them exist (e.g. `name` or `status`).
// This is dangerous as it hides accidentally undefined variables.
// We blacklist the globals that we deem potentially confusing.
// To use them, explicitly reference them, e.g. `window.name` or `window.status`.
const restrictedGlobals = require("confusing-browser-globals");

// Here are options we can add to enable jest/testing-library linting:
/*
{
  plugins: ['jest', 'testing-library'],
  overrides: [
    {
      files: ['**\/__tests__/**\/*', '**\/*.{spec,test}.*'],
      env: {
        'jest/globals': true,
      },
      // A subset of the recommended rules:
      rules: {
        // https://github.com/jest-community/eslint-plugin-jest
        'jest/no-conditional-expect': 'error',
        'jest/no-identical-title': 'error',
        'jest/no-interpolation-in-snapshots': 'error',
        'jest/no-jasmine-globals': 'error',
        'jest/no-jest-import': 'error',
        'jest/no-mocks-import': 'error',
        'jest/valid-describe-callback': 'error',
        'jest/valid-expect': 'error',
        'jest/valid-expect-in-promise': 'error',
        'jest/valid-title': 'warn',

        // https://github.com/testing-library/eslint-plugin-testing-library
        'testing-library/await-async-query': 'error',
        'testing-library/await-async-utils': 'error',
        'testing-library/no-await-sync-query': 'error',
        'testing-library/no-container': 'error',
        'testing-library/no-debugging-utils': 'error',
        'testing-library/no-dom-import': ['error', 'react'],
        'testing-library/no-node-access': 'error',
        'testing-library/no-promise-in-fire-event': 'error',
        'testing-library/no-render-in-setup': 'error',
        'testing-library/no-unnecessary-act': 'error',
        'testing-library/no-wait-for-empty-callback': 'error',
        'testing-library/no-wait-for-multiple-assertions': 'error',
        'testing-library/no-wait-for-side-effects': 'error',
        'testing-library/no-wait-for-snapshot': 'error',
        'testing-library/prefer-find-by': 'error',
        'testing-library/prefer-presence-queries': 'error',
        'testing-library/prefer-query-by-disappearance': 'error',
        'testing-library/prefer-screen-queries': 'error',
        'testing-library/render-result-naming-convention': 'error',
      },
    },
  ]
}
/*


/** @type {import('eslint').Linter.Config} */
module.exports = {
    root: true,
    parser: "@babel/eslint-parser",
    extends: [
        "plugin:import/recommended",
        "plugin:import/typescript",
        "prettier",
        "plugin:storybook/recommended",
    ],
    plugins: [
        "react",
        "testing-library",
        "unused-imports",
        "jest-dom",
        "prettier",
        "import",
        "jsx-a11y",
        "react-hooks",
    ],
    env: {
        browser: true,
        commonjs: true,
        es6: true,
        jest: true,
        node: false,
    },
    globals: {
        RequestInit: true,
        process: "readonly",
        cy: "readonly",
    },
    // NOTE: When adding rules here, you need to make sure they are compatible with
    // `typescript-eslint`, as some rules such as `no-array-constructor` aren't compatible.
    rules: {
        // http://eslint.org/docs/rules/
        "array-callback-return": "warn",
        "default-case": ["warn", { commentPattern: "^no default$" }],
        "dot-location": ["warn", "property"],
        eqeqeq: ["warn", "smart"],
        "new-parens": "warn",
        "no-array-constructor": "warn",
        "no-caller": "warn",
        "no-cond-assign": ["warn", "except-parens"],
        "no-console": ["error", { allow: ["warn", "error", "info", "trace"] }],
        "no-const-assign": "warn",
        "no-control-regex": "warn",
        "no-delete-var": "warn",
        "no-dupe-args": "warn",
        "no-dupe-class-members": "warn",
        "no-dupe-keys": "warn",
        "no-duplicate-case": "warn",
        "no-empty-character-class": "warn",
        "no-empty-pattern": "warn",
        "no-eval": "warn",
        "no-ex-assign": "warn",
        "no-extend-native": "warn",
        "no-extra-bind": "warn",
        "no-extra-label": "warn",
        "no-fallthrough": "warn",
        "no-func-assign": "warn",
        "no-implied-eval": "warn",
        "no-invalid-regexp": "warn",
        "no-iterator": "warn",
        "no-label-var": "warn",
        "no-labels": ["warn", { allowLoop: true, allowSwitch: false }],
        "no-lone-blocks": "warn",
        "no-loop-func": "warn",
        "no-mixed-operators": [
            "warn",
            {
                groups: [
                    ["&", "|", "^", "~", "<<", ">>", ">>>"],
                    ["==", "!=", "===", "!==", ">", ">=", "<", "<="],
                    ["&&", "||"],
                    ["in", "instanceof"],
                ],
                allowSamePrecedence: false,
            },
        ],
        "no-multi-str": "warn",
        "no-global-assign": "warn",
        "no-unsafe-negation": "warn",
        "no-new-func": "warn",
        "no-new-object": "warn",
        "no-new-symbol": "warn",
        "no-new-wrappers": "warn",
        "no-obj-calls": "warn",
        "no-octal": "warn",
        "no-octal-escape": "warn",
        "no-redeclare": "warn",
        "no-regex-spaces": "warn",
        "no-restricted-syntax": ["warn", "WithStatement"],
        "no-script-url": "warn",
        "no-self-assign": "warn",
        "no-self-compare": "warn",
        "no-sequences": "warn",
        "no-shadow-restricted-names": "warn",
        "no-sparse-arrays": "warn",
        "no-template-curly-in-string": "warn",
        "no-this-before-super": "warn",
        "no-throw-literal": "warn",
        "no-undef": "error",
        "no-restricted-globals": ["error"].concat(restrictedGlobals),
        "no-unreachable": "warn",
        "no-unused-expressions": [
            "error",
            {
                allowShortCircuit: true,
                allowTernary: true,
                allowTaggedTemplates: true,
            },
        ],
        "no-unused-labels": "warn",
        "no-unused-vars": [
            "warn",
            {
                args: "none",
                ignoreRestSiblings: true,
            },
        ],
        "no-use-before-define": [
            "warn",
            {
                functions: false,
                classes: false,
                variables: false,
            },
        ],
        "no-useless-computed-key": "warn",
        "no-useless-concat": "warn",
        "no-useless-constructor": "warn",
        "no-useless-escape": "warn",
        "no-useless-rename": [
            "warn",
            {
                ignoreDestructuring: false,
                ignoreImport: false,
                ignoreExport: false,
            },
        ],
        "no-with": "warn",
        "no-whitespace-before-property": "warn",
        "react-hooks/exhaustive-deps": "warn",
        "require-yield": "warn",
        "rest-spread-spacing": ["warn", "never"],
        strict: ["warn", "never"],
        "unicode-bom": ["warn", "never"],
        "use-isnan": "warn",
        "valid-typeof": "warn",
        "no-restricted-properties": [
            "error",
            {
                object: "require",
                property: "ensure",
                message:
                    "Please use import() instead. More info: https://facebook.github.io/create-react-app/docs/code-splitting",
            },
            {
                object: "System",
                property: "import",
                message:
                    "Please use import() instead. More info: https://facebook.github.io/create-react-app/docs/code-splitting",
            },
        ],
        "getter-return": "warn",

        // https://github.com/benmosher/eslint-plugin-import/tree/master/docs/rules
        "import/first": "warn",
        "import/no-amd": "error",
        "import/no-anonymous-default-export": "warn",
        "import/no-webpack-loader-syntax": "error",

        // https://github.com/yannickcr/eslint-plugin-react/tree/master/docs/rules
        "react/forbid-foreign-prop-types": ["warn", { allowInPropTypes: true }],
        "react/jsx-no-comment-textnodes": "warn",
        "react/jsx-no-duplicate-props": "warn",
        "react/jsx-no-target-blank": "warn",
        "react/jsx-no-undef": "error",
        "react/jsx-pascal-case": [
            "warn",
            {
                allowAllCaps: true,
                ignore: [],
            },
        ],
        "react/no-danger-with-children": "warn",
        // Disabled because of undesirable warnings
        // See https://github.com/facebook/create-react-app/issues/5204 for
        // blockers until its re-enabled
        // 'react/no-deprecated': 'warn',
        "react/no-direct-mutation-state": "warn",
        "react/no-is-mounted": "warn",
        "react/no-typos": "error",
        "react/require-render-return": "error",
        "react/style-prop-object": "warn",

        // https://github.com/evcohen/eslint-plugin-jsx-a11y/tree/master/docs/rules
        "jsx-a11y/alt-text": "warn",
        "jsx-a11y/anchor-has-content": "warn",
        "jsx-a11y/anchor-is-valid": [
            "warn",
            {
                aspects: ["noHref", "invalidHref"],
            },
        ],
        "jsx-a11y/aria-activedescendant-has-tabindex": "warn",
        "jsx-a11y/aria-props": "warn",
        "jsx-a11y/aria-proptypes": "warn",
        "jsx-a11y/aria-role": ["warn", { ignoreNonDOM: true }],
        "jsx-a11y/aria-unsupported-elements": "warn",
        "jsx-a11y/heading-has-content": "warn",
        "jsx-a11y/iframe-has-title": "warn",
        "jsx-a11y/img-redundant-alt": "warn",
        "jsx-a11y/no-access-key": "warn",
        "jsx-a11y/no-distracting-elements": "warn",
        "jsx-a11y/no-redundant-roles": "warn",
        "jsx-a11y/role-has-required-aria-props": "warn",
        "jsx-a11y/role-supports-aria-props": "warn",
        "jsx-a11y/scope": "warn",

        // https://github.com/facebook/react/tree/main/packages/eslint-plugin-react-hooks
        "react-hooks/rules-of-hooks": "error",

        "import/no-unresolved": "off",
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
        "react/jsx-uses-vars": "warn",
        "react/jsx-uses-react": "warn",
        //"react/react-in-jsx-scope": "error",
    },
    overrides: [
        {
            files: ["**/*.ts?(x)"],
            parser: "@typescript-eslint/parser",
            parserOptions: {
                ecmaVersion: 2018,
                sourceType: "module",
                ecmaFeatures: {
                    jsx: true,
                },

                // typescript-eslint specific options
                warnOnUnsupportedTypeScriptVersion: true,
            },
            plugins: ["@typescript-eslint"],
            // If adding a typescript-eslint version of an existing ESLint rule,
            // make sure to disable the ESLint rule here.
            rules: {
                // TypeScript's `noFallthroughCasesInSwitch` option is more robust (#6906)
                "default-case": "off",
                // 'tsc' already handles this (https://github.com/typescript-eslint/typescript-eslint/issues/291)
                "no-dupe-class-members": "off",
                // 'tsc' already handles this (https://github.com/typescript-eslint/typescript-eslint/issues/477)
                "no-undef": "off",

                // Add TypeScript specific rules (and turn off ESLint equivalents)
                "@typescript-eslint/consistent-type-assertions": "warn",
                "no-array-constructor": "off",
                "@typescript-eslint/no-array-constructor": "warn",
                "no-redeclare": "off",
                "@typescript-eslint/no-redeclare": "warn",
                "no-use-before-define": "off",
                "@typescript-eslint/no-use-before-define": [
                    "warn",
                    {
                        functions: false,
                        classes: false,
                        variables: false,
                        typedefs: false,
                    },
                ],
                "no-unused-expressions": "off",
                "@typescript-eslint/no-unused-expressions": [
                    "error",
                    {
                        allowShortCircuit: true,
                        allowTernary: true,
                        allowTaggedTemplates: true,
                    },
                ],
                "no-unused-vars": "off",
                "@typescript-eslint/no-unused-vars": [
                    "warn",
                    {
                        args: "none",
                        ignoreRestSiblings: true,
                    },
                ],
                "no-useless-constructor": "off",
                "@typescript-eslint/no-useless-constructor": "warn",
            },
        },
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
        react: {
            version: "detect",
        },
    },
    ignorePatterns: ["node_modules/", "build/", "src/reportWebVitals.js"],
};
