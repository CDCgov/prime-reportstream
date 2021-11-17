import { OktaAuth } from "@okta/okta-auth-js";

import { GLOBAL_STORAGE_KEYS } from "../components/GlobalContextProvider";

function clearGlobalContext(): void {
    for (let key in localStorage) {
        if (key.includes(GLOBAL_STORAGE_KEYS.GLOBAL_BASE)) {
            localStorage.removeItem(key);
        }
    }
}

function logout(oktaAuth: OktaAuth): void {
    if (oktaAuth?.authStateManager?._authState?.isAuthenticated) {
        clearGlobalContext();
        oktaAuth.signOut();
    }
}

export { logout };
