import { OktaAuth } from "@okta/okta-auth-js";
import { GLOBAL_STORAGE_KEYS } from "../components/GlobalContextProvider"
import { oktaAuthConfig } from "../oktaConfig";

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);

function clearGlobalContext() {
    for (let key in localStorage) {
        if (key.includes(GLOBAL_STORAGE_KEYS.GLOBAL_BASE)) localStorage.removeItem(key)
    }
}

function logout() {
    if (OKTA_AUTH.authStateManager._authState.isAuthenticated) {
        clearGlobalContext()
        OKTA_AUTH.signOut()
    }
}

export { logout }