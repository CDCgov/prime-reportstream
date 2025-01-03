module.exports = {
  env: {
    browser: false,
    es2021: true,
  },
  extends: ["eslint:recommended", "plugin:node/recommended"],
  rules: {
    "node/no-missing-import": [
      "error",
      {
        allowModules: ["@slack/bolt"],
      },
    ],
  },
};
