import { OktaAuth } from "@okta/okta-auth-js";

function logout(oktaAuth: OktaAuth): void {
    if (oktaAuth?.signOut) {
        try {
            oktaAuth.signOut().finally(() => {
                const bc = new BroadcastChannel("session");
                bc.postMessage("logout");
            });
        } catch (e) {
            console.trace(e);
        }
    }
    localStorage.clear();
}

export { logout };
