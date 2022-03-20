import { OktaAuth } from "@okta/okta-auth-js";

import { clearGlobalContext } from "../contexts/SessionStorageTools";

function logout(oktaAuth: OktaAuth): void {
    if (oktaAuth?.signOut) {
        try {
            oktaAuth.signOut();
        } catch (e) {
            console.trace(e);
        }
    }
    clearGlobalContext();
}

export { logout };
