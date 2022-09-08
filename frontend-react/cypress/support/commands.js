import "@testing-library/cypress/add-commands";

import "cypress-localstorage-commands";
import { authenticator } from "otplib";
import { OktaAuth } from "@okta/okta-auth-js";

// this will need to be dynamic as we move to CI
const REDIRECT_URI = "http://localhost:3000/login/callback";
const RESPONSE_MODE = "okta_post_message";

const oktaAuthConfig = {
    issuer: `${Cypress.env("okta_url")}/oauth2/default`,
    clientId: Cypress.env("okta_client_id"),
    redirectUri: REDIRECT_URI,
    responseMode: RESPONSE_MODE,
    tokenManager: {
        storage: "sessionStorage",
    },
    scopes: ["openid", "email"],
};

// Okta
Cypress.Commands.add("loginByOktaApi", (username, password) => {
    // log in with username and password
    cy.request("POST", "https://hhs-prime.oktapreview.com/api/v1/authn", {
        username,
        password,
        options: {
            multiOptionalFactorEnroll: false,
            warnBeforePasswordExpired: true,
        },
    })
        // pass MFA
        .then((response) => {
            const stateToken = response.body.stateToken;
            const factorId = response.body._embedded.factors[0].id;
            const secret = Cypress.env("okta_secret");
            const passCode = authenticator.generate(secret);
            return cy.request(
                "POST",
                `https://hhs-prime.oktapreview.com/api/v1/authn/factors/${factorId}/verify`,
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
            window.sessionStorage.setItem(
                "okta-token-storage",
                JSON.stringify({
                    idToken,
                    accessToken,
                })
            );
        });
});
