export default {
    "*.{js,mjs,cjs,ts,mts,cts,tsx}": [
        "eslint --fix",
        "prettier --write --list-different",
    ],
    "**/*.ts?(x)": () => "tsc",
    "*.{md,css,scss,json}": "prettier --write --check",
};
