import { OktaAuth } from "@okta/okta-auth-js";

import { clearGlobalContext } from "../components/GlobalContextProvider";


function logout(oktaAuth: OktaAuth): void {
    if (oktaAuth?.authStateManager?._authState?.isAuthenticated) {
        clearGlobalContext();
        oktaAuth.signOut();
    }
}

export { logout };
