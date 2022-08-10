// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands'

// Alternatively you can use CommonJS syntax:
// require('./commands')

export const loginHooks = () => {
    // Global before for logging in to Okta
    before(() => {
        // Clear any leftover data from previous tests
        cy.clearCookies();
        cy.clearLocalStorageSnapshot();
        // Login to Okta to get an access token
        cy.loginByOktaApi();
    });
    beforeEach(() => {
        // Cypress clears cookies by default, but for these tests
        // we want to preserve the Spring session cookie
        Cypress.Cookies.preserveOnce("SESSION");
        // It also clears local storage, so restore before each test
        cy.restoreLocalStorage();
    });
    afterEach(() => {
        cy.saveLocalStorage();
    });
};
