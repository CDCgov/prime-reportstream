import { authenticator } from "otplib";
import { OktaAuth, OktaAuthOptions } from "@okta/okta-auth-js";

import { GLOBAL_STORAGE_KEYS } from "../../../src/utils/SessionStorageTools";

declare global {
    namespace Cypress {
        interface Chainable {
            login(): Chainable<any>;
        }
    }
}

const USERNAME = Cypress.env("oktaUsername");
const PASSWORD = Cypress.env("oktaPassword");
const SECRET = Cypress.env("oktaSecret");
const CLIENT_ID = Cypress.env("oktaClientId");
const REDIRECT_URI = `${Cypress.env("baseUrl")}/login/callback`;
const RESPONSE_MODE = "okta_post_message";
const URL = Cypress.env("oktaUrl");

const oktaAuthConfig: OktaAuthOptions = {
    issuer: `${URL}/oauth2/default`,
    clientId: CLIENT_ID,
    redirectUri: REDIRECT_URI,
    responseMode: RESPONSE_MODE,
    tokenManager: {
        storage: "localStorage",
    },
    scopes: ["openid", "email"],
};

function loginByOktaApi() {
    cy.task("log", REDIRECT_URI);
    // log in with username and password
    return (
        cy
            .request("POST", `${URL}/api/v1/authn`, {
                username: USERNAME,
                password: PASSWORD,
                options: {
                    multiOptionalFactorEnroll: false,
                    warnBeforePasswordExpired: true,
                },
            })
            // pass MFA
            .then((response) => {
                const stateToken = response.body.stateToken;
                const factorId = response.body._embedded.factors[0].id;
                const passCode = authenticator.generate(SECRET);
                return cy.request(
                    "POST",
                    `${URL}/api/v1/authn/factors/${factorId}/verify`,
                    {
                        passCode,
                        stateToken,
                    }
                );
            })
            // authorize
            .then((response) => {
                const sessionToken = response.body.sessionToken;
                const authClient = new OktaAuth(oktaAuthConfig);
                return authClient.token.getWithoutPrompt({
                    sessionToken,
                });
            })
            // set tokens in storage
            .then(({ tokens }) => {
                const { accessToken, idToken } = tokens;
                const authString = JSON.stringify({
                    idToken,
                    accessToken,
                });
                window.localStorage.setItem(
                    GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN,
                    authString
                );
                cy.task("setAuth", authString);
                cy.task("log", "Successfully logged in!");
            })
    );
}

function login() {
    return cy.task("getAuth").then((auth) => {
        if (!auth) {
            cy.task(
                "log",
                "Missing stored auth information, logging in via API"
            );
            return loginByOktaApi();
        }
        cy.task("log", "Using existing auth");
        window.localStorage.setItem(
            GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN,
            auth as string
        );
    });
}

Cypress.Commands.add("login", login);
