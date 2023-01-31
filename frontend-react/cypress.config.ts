import path from "path";
import dotenv from "dotenv";
import { defineConfig } from "cypress";

const { CYPRESS_ENV: cypressEnv = "development" } = process.env;

// Populate Cypress env with values from .env.* file
dotenv.config({ path: path.resolve(process.cwd(), `./.env.${cypressEnv}`) });

const cypressConfig = require(path.resolve(
    process.cwd(),
    `./cypress/cypress.${cypressEnv}.json`
));

export default defineConfig({
    e2e: {
        baseUrl: cypressConfig.baseUrl,
        specPattern: "cypress/e2e/**/*.cy.(js|ts)",
        setupNodeEvents(on) {
            // configure plugins here
            on("task", {
                log(message) {
                    console.log(`[RS LOG] ${message}`);

                    return null;
                },
                getAuth(): string {
                    return global.auth || null;
                },
                setAuth(auth: string) {
                    global.auth = auth;

                    return null;
                },
            });
        },
    },
    env: {
        ...cypressConfig,
        okta_domain: process.env.REACT_APP_OKTA_DOMAIN,
        okta_client_id: process.env.REACT_APP_OKTA_CLIENTID,
        okta_secret: process.env.REACT_APP_OKTA_SECRET,
        okta_url: process.env.REACT_APP_OKTA_URL,

        base_page_title: process.env.REACT_APP_TITLE,
    },

    viewportHeight: 768,
    viewportWidth: 1440,
});
