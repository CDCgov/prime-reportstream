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

/*

  let's log in once here for the whole test suite
  we can store login credentials after log in 
  in memory and set them in local storage independently for each test
  using a quote unquote login function rather than actually running through the flow

  in the case our token gets stale or something, we would need a way to determine that
  we should attempt another login. mabye on a 403 based on local storage insertion
  we have an automatic full login flow retry setup
*/
const username = Cypress.env("auth_username");
const password = Cypress.env("auth_password");

const loginByOktaApi = () => {
    cy.task("log", "!!! logging in");
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
            const authString = JSON.stringify({
                idToken,
                accessToken,
            });
            window.sessionStorage.setItem("okta-token-storage", authString);
            cy.task("setAuth", authString);
            cy.task("log", "$$$ logged in");
        });
};

const login = () => {
    cy.task("getAuth").then((auth) => {
        if (!auth) {
            cy.task(
                "log",
                "Missing stored auth information, logging in via UI"
            );
            return loginByOktaApi();
        }
        cy.task("log", "logging in with existing auth");
        window.sessionStorage.setItem("okta-token-storage", auth);
    });
};

Cypress.Commands.add("login", login);
