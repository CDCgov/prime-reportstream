export default {
    /**
     * If either AppRouter or sitemap changed: Get all urls from sitemap and parse all
     * public urls from our route object and throw if they don't match.
     * @todo Implement logic and remove approuter-check.sh once done.
     */
    "{src/AppRouter.tsx,public/sitemap.xml}": (filenames) => "echo 'TODO'",
    /* Linting and formatting */
    "*.{js,mjs,cjs,ts,mts,cts,tsx}": ["eslint --fix", "prettier --write --list-different"],
    "*.{md,css,scss,json}": "prettier --write --check",
    /**
     * If either yarn.lock or package.json changed: run dedupe, manually add yarn.lock
     * (in case it wasn't already staged).
     */
    "{yarn.lock, package.json}": [() => "yarn dedupe", () => "git add frontend-react/yarn.lock"],
    /**
     * Determine if whole project needs testing or not
     */
    "{**/*.{ts,tsx},yarn.lock,package.json}": (/** @type string[] */ filenames) => {
        // if dependencies changed, test the whole project
        if (filenames.some((f) => /(package.json)|(yarn.lock)$/.exec(f))) {
            return "cross-env TZ=UTC vitest --run --silent";
        }
        // otherwise test only related
        return `cross-env TZ=UTC vitest related --run --silent ${filenames.filter((f) => /\.tsx?/.exec(f).join(" "))}`;
    },
};
