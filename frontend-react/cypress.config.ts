import { defineConfig } from "cypress";

// Populate process.env with values from .env file
require("dotenv").config();
export default defineConfig({
    e2e: {
        baseUrl: "http://localhost:3000",
        specPattern: "cypress/integration/**/*.test.js",
    },
    env: {
        auth_username: process.env.REACT_APP_OKTA_USERNAME,
        auth_password: process.env.REACT_APP_OKTA_PASSWORD,
        okta_domain: process.env.REACT_APP_OKTA_DOMAIN,
        okta_client_id: process.env.REACT_APP_OKTA_CLIENTID,
        okta_secret: process.env.REACT_APP_OKTA_SECRET,
        okta_url: process.env.REACT_APP_OKTA_URL,
    },
});
