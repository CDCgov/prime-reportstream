import { OKTA_AUTH } from "../oktaConfig";

export enum SessionEvent {
    LOGIN = "login",
    LOGOUT = "logout",
}

async function logout(): Promise<void> {
    /*
     * Clear localstorage BEFORE okta signout as
     * the okta process will prevent logic after
     * from running
     */
    localStorage.clear();
    try {
        await OKTA_AUTH.signOut();
    } catch (e) {
        console.trace(e);
    }
}

export { logout };
