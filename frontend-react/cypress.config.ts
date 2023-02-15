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

function getEnvOrDefault(name: string, dfault?: string) {
    if (!!cypressConfig[name]) {
        return cypressConfig[name];
    }

    if (dfault) {
        return dfault;
    }

    throw new Error(`No value supplied for env variable ${name}`);
}

const env = {
    ...cypressConfig,
    oktaClientId: getEnvOrDefault(
        "oktaClientId",
        process.env.REACT_APP_OKTA_CLIENTID
    ),
    oktaSecret: getEnvOrDefault(
        "oktaSecret",
        process.env.REACT_APP_OKTA_SECRET
    ),
    oktaUrl: getEnvOrDefault("oktaUrl", process.env.REACT_APP_OKTA_URL),
    baseUrl: getEnvOrDefault("baseUrl", process.env.REACT_APP_BASE_URL),
    basePageTitle: process.env.REACT_APP_TITLE,
};

const env2 = {
    oktaUsername: "stephenkao+1@navapbc.com",
    oktaPassword: "yfe7umk-hwj-bhu*WJW",
    oktaSecret: "SGTLTZWM6CUC7CKS",
    oktaClientId: "0oa2fs6vp3W5MTzjh1d7",
    oktaIssuer: "https://hhs-prime.oktapreview.com/oauth2/default",
    oktaUrl: "https://hhs-prime.oktapreview.com",
    baseUrl: "http://localhost:3000",
};

export default defineConfig({
    e2e: {
        baseUrl: env.baseUrl,
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
    env,
    viewportHeight: 768,
    viewportWidth: 1440,
});
