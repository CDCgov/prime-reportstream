// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
import "@testing-library/cypress/add-commands";

import {OktaAuth} from '@okta/okta-auth-js';

import "cypress-localstorage-commands";
import {authenticator} from "otplib";

// Okta
Cypress.Commands.add('loginByOktaApi', (username, password) => {
    cy.request("POST", "https://hhs-prime.oktapreview.com/api/v1/authn", {
        username,
        password,
        options: {
            multiOptionalFactorEnroll: true,
            warnBeforePasswordExpired: true,
        },
    }).then((response) => {
        const stateToken = response.body.stateToken;
        const factorId = response.body._embedded.factors[0].id;
        // const passCode = authenticator.generate(Cypress.env("okta_secret"));
        const passCode = authenticator.generate("secret");
        cy.request(
            "POST",
            `https://hhs-prime.oktapreview.com/api/v1/authn/factors/${factorId}/verify`,
            {
                passCode,
                stateToken,
            }
        ).then((response) => {
            const sessionToken = response.body.sessionToken;
            cy.request(
                "GET",
                `https://hhs-prime.oktapreview.com/oauth2/default/v1/authorize?client_id=${Cypress.env('okta_client_id')}&redirect_uri=http%3A%2F%2Flocalhost.reportstream.gov%2F&response_type=token%20id_token&scope=openid%20email&nonce=thisisnotsafe&state=thisisbogus&sessionToken=${sessionToken}`
            ).then((response) => {
                const redirect = response.redirects[0];
                const idTokenRegex = new RegExp(
                    "(?:id_token=)((.[\\s\\S]*))(?:&access_token=)",
                    "ig"
                );
                const accessTokenRegex = new RegExp(
                    "(?:access_token=)((.[\\s\\S]*))(?:&token_type=Bearer)",
                    "ig"
                );
                const id_token = idTokenRegex.exec(redirect)[1];
                const access_token = accessTokenRegex.exec(redirect)[1];
                cy.task("setAuth", {id_token, access_token});
                cy.setLocalStorage("id_token", id_token);
                cy.setLocalStorage("access_token", access_token);
                cy.saveLocalStorage();
            });
        });
    });
});