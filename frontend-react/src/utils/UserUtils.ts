import { OktaAuth } from "@okta/okta-auth-js";

export enum SessionEvent {
    LOGIN = "login",
    LOGOUT = "logout",
}

async function logout(oktaAuth: OktaAuth): Promise<void> {
    if (oktaAuth?.signOut) {
        try {
            await oktaAuth.signOut();
        } catch (e) {
            console.trace(e);
        }
    }
    localStorage.clear();
}

export { logout };
