import { OktaAuth } from "@okta/okta-auth-js";

function logout(oktaAuth: OktaAuth): void {
    if (oktaAuth?.signOut) {
        try {
            oktaAuth.signOut();
        } catch (e) {
            console.trace(e);
        }
    }
    // maybe let's clear the app state here too?
    sessionStorage.clear();
}

export { logout };
